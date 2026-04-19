package org.example.shield.consultation.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.ChecklistCoverageService;
import org.example.shield.ai.application.CohereService;
import org.example.shield.ai.application.RagPipelineService;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.exception.ChatAiException;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.controller.dto.MessageResponse;
import org.example.shield.consultation.controller.dto.SendMessageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.example.shield.consultation.domain.MessageWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MessageService {

    private final MessageReader messageReader;
    private final MessageWriter messageWriter;
    private final ConsultationReader consultationReader;
    private final ConsultationWriter consultationWriter;
    private final CohereService cohereService;
    private final CohereApiConfig cohereApiConfig;
    private final SanitizeService sanitizeService;
    private final ChecklistCoverageService checklistCoverageService;
    private final RagPipelineService ragPipelineService;

    @Transactional
    public SendMessageResponse sendMessage(UUID consultationId, String content) {
        Consultation consultation = consultationReader.findById(consultationId);

        // 0. 사용자 입력 sanitization (P0-III)
        String sanitizedText;
        try {
            sanitizedText = sanitizeService.sanitizeUserText(content);
        } catch (SanitizeService.PiiDetectedException e) {
            Message piiMessage = Message.createAiMessage(
                    consultationId, e.getMessage(), null, null, null, null);
            Message savedPii = messageWriter.save(piiMessage);
            return SendMessageResponse.from(savedPii, false);
        }

        // 1. USER 메시지 저장 (원문 보존)
        Message userMessage = Message.createUserMessage(consultationId, content);
        messageWriter.save(userMessage);

        // 대화 내역 1회 조회 — RAG와 chat() 양쪽에서 공유 (중복 DB 쿼리 방지)
        List<Message> chatHistory = messageReader.findAllByConsultationId(consultationId);

        // [RAG] 도메인 정보가 있을 때만 실행
        String ragContext = "";
        String domainForRag = consultation.getFirstDomain();
        if (domainForRag != null) {
            ragContext = ragPipelineService.execute(chatHistory, domainForRag, consultationId);
        }

        // 2. Cohere API 호출 (Phase 1 대화 — RAG 컨텍스트 포함, 조회된 chatHistory 재사용)
        AiCallResult<ChatParsedResponse> result = cohereService.chat(
                consultation, sanitizedText, ragContext, chatHistory);
        ChatParsedResponse parsed = result.data();

        // 3. 응답 ID 저장 (감사 로깅용)
        consultation.updateLastResponseId(result.responseId());

        // 3-1. AI 응답 blank 차단 (Issue #45)
        //      — nextQuestion 이 비어있으면 사용자에게 의미 있는 응답을 만들 수 없고,
        //        이 상태로 DB에 저장되면 이후 Cohere v2 Chat API 가 빈 assistant
        //        content 를 400 으로 거부해 대화가 영구적으로 막힌다.
        String nextQuestion = parsed.getNextQuestion();
        if (nextQuestion == null || nextQuestion.isBlank()) {
            log.error("AI chat response is blank: consultationId={}, responseId={}, tokensOut={}",
                    consultationId, result.responseId(), result.tokensOutput());
            throw new ChatAiException();
        }

        // 4. AI 분류 결과 처리 (primaryFieldLocked 가드)
        if (hasAny(parsed.getAiDomains()) || hasAny(parsed.getAiSubDomains()) || hasAny(parsed.getAiTags())) {
            boolean updated = consultation.updateAiClassification(
                    parsed.getAiDomains(), parsed.getAiSubDomains(), parsed.getAiTags());
            if (!updated) {
                log.warn("LLM attempted to override locked classification: consultationId={}, aiDomains={}",
                        consultationId, parsed.getAiDomains());
            }
        }

        // 5. AI 메시지 저장
        Message aiMessage = Message.createAiMessage(
                consultationId,
                nextQuestion,
                cohereApiConfig.getChatModel(),  // model name from config
                result.tokensInput(),
                result.tokensOutput(),
                result.latencyMs()
        );
        Message savedAi = messageWriter.save(aiMessage);

        // 6. allCompleted AND gate (P0-II, Issue #40 3레벨 커버리지)
        boolean effectiveAllCompleted = false;
        if (parsed.isAllCompleted()) {
            String l1 = consultation.getFirstDomain();
            String l2 = consultation.getFirstSubDomain();
            String l3 = consultation.getFirstTag();

            double coverageRatio = checklistCoverageService.compute(
                    consultationId, l1, l2, l3);
            effectiveAllCompleted = checklistCoverageService.isEffectivelyCompleted(
                    true, coverageRatio);

            if (!effectiveAllCompleted) {
                log.warn("LLM reported allCompleted=true but coverage={} < {}: consultationId={}, L1={}, L2={}, L3={}",
                        coverageRatio, checklistCoverageService.getThreshold(), consultationId,
                        l1, l2, l3);
            }
        }

        // 7. 상담 마지막 메시지 + updatedAt 갱신
        consultation.updateLastMessage(savedAi.getContent(), savedAi.getCreatedAt());
        consultation.touch();
        consultationWriter.save(consultation);

        return SendMessageResponse.from(savedAi, effectiveAllCompleted);
    }

    public PageResponse<MessageResponse> getMessages(UUID consultationId, Pageable pageable) {
        consultationReader.findById(consultationId);

        Page<Message> messages = messageReader.findAllByConsultationId(consultationId, pageable);
        Page<MessageResponse> responsePage = messages.map(MessageResponse::from);

        return PageResponse.from(responsePage);
    }

    private boolean hasAny(List<String> list) {
        return list != null && !list.isEmpty();
    }
}
