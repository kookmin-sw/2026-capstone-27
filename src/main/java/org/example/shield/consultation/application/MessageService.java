package org.example.shield.consultation.application;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.ChecklistCoverageService;
import org.example.shield.ai.application.CohereService;
import org.example.shield.ai.application.OntologyService;
import org.example.shield.ai.application.RagPipelineService;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.exception.ChatAiException;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.controller.dto.MessageResponse;
import org.example.shield.consultation.controller.dto.SendMessageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 상담 메시지 처리 조율자.
 *
 * <p>이 서비스는 <b>non-transactional</b> 이다 ({@code readOnly=true} 도 부여하지
 * 않는다). Cohere Chat v2 호출은 외부 HTTP 요청이라 DB 트랜잭션 안에서 수행
 * 되면 커넥션을 수초~수십초 점유할 수 있으므로, DB 작업은
 * {@link ChatTransactionalBoundary} 의 짧은 트랜잭션으로 분리했다.</p>
 *
 * <p>실행 흐름:</p>
 * <ol>
 *   <li>사용자 입력 sanitize (순수 로직, 예외 시 PII 안내 메시지 저장 후 early return)</li>
 *   <li>USER 메시지 저장 — {@link ChatTransactionalBoundary#saveUserMessage} (독립 tx)</li>
 *   <li>대화 내역 조회 (tx 밖의 read-only)</li>
 *   <li>RAG + Cohere chat() 호출 — <b>트랜잭션 밖</b></li>
 *   <li>blank 응답 차단 — {@link ChatAiException} (Issue #45)</li>
 *   <li>AI 분류 결과 온톨로지 필터링 (순수 로직)</li>
 *   <li>AI 응답 최종 반영 — {@link ChatTransactionalBoundary#finalizeAiResponse} (독립 tx)</li>
 *   <li>allCompleted 커버리지 게이트 (외부 검사, tx 불필요)</li>
 * </ol>
 *
 * <p>USER 메시지 저장이 독립 트랜잭션이기 때문에 이후 Cohere 실패·blank 응답·
 * 네트워크 에러 등 어떤 예외가 발생해도 사용자 입력은 절대 유실되지 않는다.
 * PR-A 의 {@code noRollbackFor} 접근보다 한 단계 더 강한 격리다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageReader messageReader;
    private final ConsultationReader consultationReader;
    private final CohereService cohereService;
    private final CohereApiConfig cohereApiConfig;
    private final SanitizeService sanitizeService;
    private final ChecklistCoverageService checklistCoverageService;
    private final RagPipelineService ragPipelineService;
    private final OntologyService ontologyService;
    private final ChatTransactionalBoundary chatTxBoundary;
    private final ChatMetrics chatMetrics;

    /**
     * 사용자 메시지 처리 및 AI 응답 생성.
     *
     * <p>{@code noRollbackFor = ChatAiException.class} (Issue #45 후속):
     * AI 응답이 blank 로 내려와 {@link ChatAiException} 이 발생하더라도
     * 사용자가 이미 저장한 USER 메시지와 감사 로깅용
     * {@code lastResponseId} 는 커밋되어야 한다. 그렇지 않으면 AI 실패
     * 시마다 사용자 입력이 유실되어 재현 가능한 데이터 손실이 발생한다.</p>
     *
     * <p>ChatAiException 이외의 런타임 예외는 기존 기본 동작(전체 롤백)
     * 을 유지하므로 RAG/Cohere 호출 실패는 그대로 롤백된다.</p>
     */
    @Transactional(noRollbackFor = ChatAiException.class)
    public SendMessageResponse sendMessage(UUID consultationId, String content) {
        long pipelineStart = System.nanoTime();
        try {
            Consultation consultation = consultationReader.findById(consultationId);

            // 0. 사용자 입력 sanitization (P0-III)
            String sanitizedText;
            try {
                sanitizedText = sanitizeService.sanitizeUserText(content);
            } catch (SanitizeService.PiiDetectedException e) {
                Message savedPii = chatTxBoundary.savePiiAiMessage(consultationId, e.getMessage());
                chatMetrics.recordSendMessage(pipelineStart, "pii");
                return SendMessageResponse.from(savedPii, false);
            }

            // 1. USER 메시지 저장 (독립 트랜잭션 — 후속 실패와 무관하게 보존)
            chatTxBoundary.saveUserMessage(consultationId, content);

            // 대화 내역 1회 조회 — RAG와 chat() 양쪽에서 공유 (중복 DB 쿼리 방지)
            List<Message> chatHistory = messageReader.findAllByConsultationId(consultationId);

            // 2. [RAG] 도메인 정보가 있을 때만 실행 — 트랜잭션 밖
            String ragContext = "";
            String domainForRag = consultation.getFirstDomain();
            if (domainForRag != null) {
                ragContext = ragPipelineService.execute(chatHistory, domainForRag, consultationId);
            }

            // 3. Cohere Chat v2 호출 — 트랜잭션 밖 + Micrometer 타이밍
            AiCallResult<ChatParsedResponse> result = callCohereMeasured(
                    consultation, sanitizedText, ragContext, chatHistory);
            ChatParsedResponse parsed = result.data();

            // 4. AI 응답 blank 차단 (Issue #45)
            String nextQuestion = parsed.getNextQuestion();
            if (nextQuestion == null || nextQuestion.isBlank()) {
                log.error("AI chat response is blank: consultationId={}, responseId={}, tokensOut={}",
                        consultationId, result.responseId(), result.tokensOutput());
                chatMetrics.incrementBlankResponse();
                chatMetrics.recordSendMessage(pipelineStart, "blank");
                throw new ChatAiException();
            }

            // 5. AI 분류 결과 온톨로지 필터링 (순수 로직)
            List<String> validSubs = null;
            List<String> validTags = null;
            boolean hasAnyAi = hasAny(parsed.getAiDomains())
                    || hasAny(parsed.getAiSubDomains())
                    || hasAny(parsed.getAiTags());
            if (hasAnyAi) {
                validSubs = filterValidChildren(
                        parsed.getAiSubDomains(),
                        firstOrNull(consultation.getUserDomains()),
                        consultationId,
                        "L2");
                List<String> l2Ref = hasAny(consultation.getUserSubDomains())
                        ? consultation.getUserSubDomains()
                        : validSubs;
                validTags = filterValidChildren(
                        parsed.getAiTags(),
                        firstOrNull(l2Ref),
                        consultationId,
                        "L3");
            }

            // 6. AI 응답 최종 반영 (독립 트랜잭션)
            AiFinalizePayload payload = new AiFinalizePayload(
                    result.responseId(),
                    nextQuestion,
                    cohereApiConfig.getChatModel(),
                    result.tokensInput(),
                    result.tokensOutput(),
                    result.latencyMs(),
                    hasAnyAi ? parsed.getAiDomains() : null,
                    validSubs,
                    validTags
            );
            Message savedAi = chatTxBoundary.finalizeAiResponse(consultationId, payload);

            // DB에 반영된 AI 분류 결과를 로컬 객체에도 동기화하여 후속 커버리지 계산에 사용
            if (payload.hasAnyClassification()) {
                consultation.updateAiClassification(payload.aiDomains(), payload.aiSubDomains(), payload.aiTags());
            }

            // 7. allCompleted AND gate (P0-II, Issue #40 3레벨 커버리지) — 트랜잭션 밖
            boolean effectiveAllCompleted = evaluateAllCompletedGate(consultationId, consultation, parsed);

            chatMetrics.recordSendMessage(pipelineStart, "success");
            return SendMessageResponse.from(savedAi, effectiveAllCompleted);
        } catch (ChatAiException e) {
            throw e; // already metered
        } catch (RuntimeException e) {
            chatMetrics.recordSendMessage(pipelineStart, "error");
            throw e;
        }
    }

    /**
     * Cohere chat() 호출을 Micrometer timer 로 감싼다.
     * outcome 태그: success / blank / failure.
     */
    private AiCallResult<ChatParsedResponse> callCohereMeasured(
            Consultation consultation, String sanitizedText, String ragContext, List<Message> chatHistory) {
        Timer.Sample sample = chatMetrics.startCohereCall();
        try {
            AiCallResult<ChatParsedResponse> result = cohereService.chat(
                    consultation, sanitizedText, ragContext, chatHistory);
            String nq = result.data() == null ? null : result.data().getNextQuestion();
            if (nq == null || nq.isBlank()) {
                chatMetrics.stopCohereCallBlank(sample);
            } else {
                chatMetrics.stopCohereCallSuccess(sample);
            }
            return result;
        } catch (RuntimeException e) {
            chatMetrics.stopCohereCallFailure(sample);
            throw e;
        }
    }

    /**
     * allCompleted 커버리지 AND 게이트 — DB read-only 계산만 수행.
     * {@link ChecklistCoverageService} 자체가 필요 시 readOnly 트랜잭션을 연다.
     */
    private boolean evaluateAllCompletedGate(UUID consultationId, Consultation consultation,
                                              ChatParsedResponse parsed) {
        if (!parsed.isAllCompleted()) return false;

        String l1 = consultation.getFirstDomain();
        String l2 = consultation.getFirstSubDomain();
        String l3 = consultation.getFirstTag();

        double coverageRatio = checklistCoverageService.compute(consultationId, l1, l2, l3);
        boolean effective = checklistCoverageService.isEffectivelyCompleted(true, coverageRatio);

        if (!effective) {
            log.warn("LLM reported allCompleted=true but coverage={} < {}: consultationId={}, L1={}, L2={}, L3={}",
                    coverageRatio, checklistCoverageService.getThreshold(), consultationId, l1, l2, l3);
        }
        return effective;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(UUID consultationId, Pageable pageable) {
        consultationReader.findById(consultationId);

        Page<Message> messages = messageReader.findAllByConsultationId(consultationId, pageable);
        Page<MessageResponse> responsePage = messages.map(MessageResponse::from);

        return PageResponse.from(responsePage);
    }

    private boolean hasAny(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * LLM 이 반환한 하위 분류 중 온톨로지상 parentName 의 직계 자식인 것만 남긴다.
     * parentName 이 null 이면 검증을 건너뛴다 (부모 자체가 미정이면 환각 판정 불가).
     */
    private List<String> filterValidChildren(List<String> aiNodes, String parentName,
                                             UUID consultationId, String level) {
        if (!hasAny(aiNodes)) return null;
        if (parentName == null) return aiNodes;

        List<String> valid = aiNodes.stream()
                .filter(name -> ontologyService.isChildOf(name, parentName))
                .toList();

        if (valid.size() < aiNodes.size()) {
            log.warn("온톨로지 위반 {} 제거: consultationId={}, parent={}, 원본={}, 유효={}",
                    level, consultationId, parentName, aiNodes, valid);
        }
        return valid.isEmpty() ? null : valid;
    }

    private static String firstOrNull(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}
