package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.dto.CohereChatRequest;
import org.example.shield.ai.infrastructure.CohereClient;
import org.example.shield.ai.infrastructure.GuardrailFilter;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 서비스 — AiClient를 활용한 AI 기능.
 *
 * ChatService / AnalysisService에서 호출.
 * - chat(): 대화 API (Phase 1) — 항상 full history 전송
 * - generateBrief(): 의뢰서 생성 API (Phase 2)
 * - callClassify(): RAG Layer 1 의도 분류
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CohereService {

    private final AiClient aiClient;
    private final CohereApiConfig config;
    private final PromptService promptService;
    private final SanitizeService sanitizeService;
    private final GuardrailFilter guardrailFilter;
    private final MessageReader messageReader;
    private final CohereClient cohereClient;
    private final OntologyService ontologyService;

    /**
     * Phase 1 대화 — 사용자 메시지 처리 후 AI 응답 반환.
     * Cohere v2 Chat API는 무상태 모드이므로 항상 full history 전송.
     *
     * @param consultation      상담 엔티티
     * @param sanitizedUserText 사용자 입력 (sanitize 완료)
     * @param ragContext        RAG 컨텍스트 (빈 문자열이면 미삽입)
     * @param chatHistory       이미 조회된 대화 내역 (중복 DB 쿼리 방지)
     * @return AiCallResult<ChatParsedResponse>
     */
    public AiCallResult<ChatParsedResponse> chat(Consultation consultation, String sanitizedUserText,
                                                  String ragContext, List<Message> chatHistory) {
        List<CohereChatRequest.Message> messages = buildChatMessages(consultation, sanitizedUserText, ragContext, chatHistory);
        AiCallResult<ChatParsedResponse> result = aiClient.callChat(
                config.getChatModel(), messages);

        // Layer 2 가드레일: 금칙어 필터
        ChatParsedResponse filtered = guardrailFilter.filterChatResponse(result.data());
        return new AiCallResult<>(
                result.responseId(),
                filtered,
                result.tokensInput(),
                result.tokensOutput(),
                result.latencyMs()
        );
    }

    /**
     * Phase 2 의뢰서 생성 — 전체 대화를 기반으로 구조화된 의뢰서 생성.
     *
     * @param consultation 상담 엔티티
     * @return AiCallResult<BriefParsedResponse>
     */
    public AiCallResult<BriefParsedResponse> generateBrief(Consultation consultation) {
        List<CohereChatRequest.Message> messages = buildBriefMessages(consultation);
        AiCallResult<BriefParsedResponse> result = aiClient.callBrief(
                config.getBriefModel(), messages);

        // Layer 2 가드레일: 의뢰서 금칙어 필터
        BriefParsedResponse filtered = guardrailFilter.filterBriefResponse(result.data());
        return new AiCallResult<>(
                result.responseId(),
                filtered,
                result.tokensInput(),
                result.tokensOutput(),
                result.latencyMs()
        );
    }

    /**
     * RAG Layer 1 — 의도 분류 전용 LLM 호출.
     * CohereClient.callRawJson()에 위임하여 raw JSON 문자열로 반환.
     *
     * @param messages 분류용 messages[] 배열 (system + user)
     * @return AiCallResult<String> — raw JSON 문자열
     */
    public AiCallResult<String> callClassify(List<CohereChatRequest.Message> messages) {
        CohereChatRequest request = CohereChatRequest.forClassify(config.getClassifyModel(), messages);
        return cohereClient.callRawJson(request, Duration.ofMillis(config.getClassifyReadTimeout()));
    }

    /**
     * Phase 1 대화용 messages[] 배열 구성.
     * system + assistant/user 턴을 역할 배열로 구조화.
     * history truncation: 시스템 프롬프트 + 최대 N턴 (configurable).
     *
     * @param chatHistory 호출자가 이미 조회한 대화 내역 (중복 DB 쿼리 방지)
     */
    private List<CohereChatRequest.Message> buildChatMessages(
            Consultation consultation, String latestSanitizedUserText,
            String ragContext, List<Message> chatHistory) {

        List<CohereChatRequest.Message> msgs = new ArrayList<>();

        // 1. 시스템 프롬프트
        String systemPrompt = promptService.loadRouterChatPrompt();

        // 분류 완료 시 체크리스트 YAML 동적 주입
        String domain = consultation.getFirstDomain();
        if (domain != null) {
            String checklist = promptService.loadChecklist(domain);
            if (checklist != null) {
                systemPrompt = systemPrompt + "\n\n" + checklist;
            }
        }

        // 분류 컨텍스트: 사용자 사전 선택 + 허용 자식 목록 (Issue #48)
        String classificationContext = buildClassificationContext(consultation);
        if (!classificationContext.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + classificationContext;
        }

        // RAG Layer 3: 법률 조문 컨텍스트 주입
        if (ragContext != null && !ragContext.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + ragContext;
        }

        msgs.add(CohereChatRequest.Message.system(systemPrompt));

        // 2. 기존 대화 내역 (시간순) — 호출자가 전달한 리스트 사용
        //    방어적 skip: 과거 DB 에 이미 저장된 빈 content 메시지가 섞여 있을 수 있음.
        //    Cohere v2 Chat API 는 빈 content 를 400 으로 거부하므로 history 구성
        //    시점에서 제외해야 한다. (Issue #45)
        for (Message msg : chatHistory) {
            if (msg.getRole() == MessageRole.USER) {
                // TODO: 저장 시점에 sanitize된 텍스트를 별도 필드에 보관하면 중복 sanitize 제거 가능
                String sanitized = sanitizeService.sanitizeUserText(msg.getContent());
                if (sanitized == null || sanitized.isBlank()) {
                    log.warn("Skipping blank USER message in chat history: messageId={}", msg.getId());
                    continue;
                }
                msgs.add(CohereChatRequest.Message.user(sanitized));
            } else if (msg.getRole() == MessageRole.CHATBOT) {
                String content = msg.getContent();
                if (content == null || content.isBlank()) {
                    log.warn("Skipping blank CHATBOT message in chat history: messageId={}", msg.getId());
                    continue;
                }
                msgs.add(CohereChatRequest.Message.assistant(content));
            }
        }

        // 3. 새 사용자 메시지 (이미 sanitize됨)
        msgs.add(CohereChatRequest.Message.user(latestSanitizedUserText));

        // 4. History truncation: system prompt + last N messages
        return truncateMessages(msgs, config.getMaxHistoryMessages());
    }

    /**
     * Phase 2 의뢰서용 messages[] 배열 구성.
     */
    private List<CohereChatRequest.Message> buildBriefMessages(Consultation consultation) {
        List<CohereChatRequest.Message> msgs = new ArrayList<>();

        // 1. 의뢰서 전용 시스템 프롬프트
        String systemPrompt = promptService.loadRouterBriefPrompt();
        msgs.add(CohereChatRequest.Message.system(systemPrompt));

        // 2. 전체 대화 내역
        //    방어적 skip: 빈 content 메시지는 Cohere v2 Chat API 가 400 으로 거부.
        //    (Issue #45)
        List<Message> history = messageReader.findAllByConsultationId(consultation.getId());
        for (Message msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                String sanitized = sanitizeService.sanitizeUserText(msg.getContent());
                if (sanitized == null || sanitized.isBlank()) {
                    log.warn("Skipping blank USER message in brief history: messageId={}", msg.getId());
                    continue;
                }
                msgs.add(CohereChatRequest.Message.user(sanitized));
            } else if (msg.getRole() == MessageRole.CHATBOT) {
                String content = msg.getContent();
                if (content == null || content.isBlank()) {
                    log.warn("Skipping blank CHATBOT message in brief history: messageId={}", msg.getId());
                    continue;
                }
                msgs.add(CohereChatRequest.Message.assistant(content));
            }
        }

        return truncateMessages(msgs, config.getMaxHistoryMessages());
    }

    /**
     * 분류 컨텍스트 프롬프트 구성 (Issue #48).
     *
     * <p>사용자가 선택한 레벨은 "재분류 금지"로 명시하고, 비워둔
     * 레벨은 온톨로지 허용 자식 목록을 주입한다. LLM 이 환각 L2/L3 을
     * 반환하는 것을 사전 차단하고 분류 정확도를 올린다.</p>
     *
     * <p>userDomains 가 비어있으면 빈 문자열 반환 — L1 조차 아직 못정하면
     * 허용 자식 목록을 만들 기준이 없어 주입 skip.</p>
     */
    private String buildClassificationContext(Consultation c) {
        List<String> userL1 = c.getUserDomains();
        if (userL1 == null || userL1.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("## 사용자 사전 선택 분류\n");
        sb.append("- 대분류: ").append(String.join(", ", userL1)).append("\n");

        boolean hasL2 = isNonEmpty(c.getUserSubDomains());
        boolean hasL3 = isNonEmpty(c.getUserTags());

        if (hasL2) sb.append("- 중분류: ").append(String.join(", ", c.getUserSubDomains())).append("\n");
        if (hasL3) sb.append("- 소분류: ").append(String.join(", ", c.getUserTags())).append("\n");

        sb.append("\n## 분류 제약 (엄수)\n");
        sb.append("- 대분류는 재분류하지 마세요 (사용자 확정).\n");

        if (!hasL2) {
            List<String> allowedL2 = ontologyService.childrenOf(userL1.get(0));
            sb.append("- aiSubDomains는 아래 목록에서만 선택:\n");
            sb.append("  ").append(allowedL2).append("\n");
        } else {
            sb.append("- 중분류는 재분류하지 마세요 (사용자 확정).\n");
        }

        if (!hasL3) {
            String l2Ref = hasL2 ? c.getUserSubDomains().get(0) : "확정될 aiSubDomains";
            sb.append("- aiTags는 '").append(l2Ref).append("'의 직계 자식(L3)에서만 선택하세요.\n");
        } else {
            sb.append("- 소분류는 재분류하지 마세요 (사용자 확정).\n");
        }

        sb.append("\n사용자 선택과 중복되는 질문은 하지 마세요.");
        return sb.toString();
    }

    private static boolean isNonEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * History truncation: 시스템 프롬프트(첫 메시지) + 최근 maxMessages개 유지.
     */
    private List<CohereChatRequest.Message> truncateMessages(List<CohereChatRequest.Message> messages, int maxMessages) {
        if (messages.size() <= maxMessages + 1) {
            return messages;
        }
        int dropped = messages.size() - maxMessages - 1;
        log.warn("History truncation: 전체 {}건 중 {}건 삭제, 최근 {}건 유지",
                messages.size() - 1, dropped, maxMessages);
        List<CohereChatRequest.Message> truncated = new ArrayList<>();
        truncated.add(messages.get(0)); // system prompt
        truncated.addAll(messages.subList(messages.size() - maxMessages, messages.size()));
        return truncated;
    }
}
