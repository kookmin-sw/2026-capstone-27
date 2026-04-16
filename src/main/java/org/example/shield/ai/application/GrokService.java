package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.GroqApiConfig;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.ai.dto.GroqRequest;
import org.example.shield.ai.infrastructure.GuardrailFilter;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 서비스 — AiClient를 활용한 AI 기능.
 *
 * ChatService / AnalysisService에서 호출.
 * - chat(): 대화 API (Phase 1) — 항상 full history 전송
 * - generateBrief(): 의뢰서 생성 API (Phase 2)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrokService {

    private final AiClient aiClient;
    private final GroqApiConfig config;
    private final PromptService promptService;
    private final SanitizeService sanitizeService;
    private final GuardrailFilter guardrailFilter;
    private final MessageReader messageReader;

    /**
     * Phase 1 대화 — 사용자 메시지 처리 후 AI 응답 반환.
     * Groq는 Stateful 모드를 지원하지 않으므로 항상 full history 전송.
     *
     * @param consultation 상담 엔티티
     * @param sanitizedUserText 사용자 입력 (sanitize 완료)
     * @return GrokCallResult<ChatParsedResponse>
     */
    public GrokCallResult<ChatParsedResponse> chat(Consultation consultation, String sanitizedUserText) {
        List<GroqRequest.Message> messages = buildChatMessages(consultation, sanitizedUserText);
        GrokCallResult<ChatParsedResponse> result = aiClient.callChat(
                config.getChatModel(), messages);

        // Layer 2 가드레일: 금칙어 필터
        ChatParsedResponse filtered = guardrailFilter.filterChatResponse(result.data());
        return new GrokCallResult<>(
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
     * @return GrokCallResult<BriefParsedResponse>
     */
    public GrokCallResult<BriefParsedResponse> generateBrief(Consultation consultation) {
        List<GroqRequest.Message> messages = buildBriefMessages(consultation);
        GrokCallResult<BriefParsedResponse> result = aiClient.callBrief(
                config.getBriefModel(), messages);

        // Layer 2 가드레일: 의뢰서 금칙어 필터
        BriefParsedResponse filtered = guardrailFilter.filterBriefResponse(result.data());
        return new GrokCallResult<>(
                result.responseId(),
                filtered,
                result.tokensInput(),
                result.tokensOutput(),
                result.latencyMs()
        );
    }

    /**
     * Phase 1 대화용 messages[] 배열 구성.
     * system + assistant/user 턴을 역할 배열로 구조화.
     * history truncation: 시스템 프롬프트 + 최대 N턴 (configurable).
     */
    private List<GroqRequest.Message> buildChatMessages(
            Consultation consultation, String latestSanitizedUserText) {

        List<GroqRequest.Message> msgs = new ArrayList<>();

        // 1. 시스템 프롬프트
        String systemPrompt = promptService.loadRouterChatPrompt();

        // 분류 완료 시 체크리스트 YAML 동적 주입
        if (consultation.getPrimaryField() != null && !consultation.getPrimaryField().isEmpty()) {
            String domain = consultation.getPrimaryField().get(0);
            String checklist = promptService.loadChecklist(domain);
            if (checklist != null) {
                systemPrompt = systemPrompt + "\n\n" + checklist;
            }
        }

        msgs.add(GroqRequest.Message.system(systemPrompt));

        // 2. 기존 대화 내역 (시간순)
        List<Message> history = messageReader.findAllByConsultationId(consultation.getId());
        for (Message msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                msgs.add(GroqRequest.Message.user(
                        sanitizeService.sanitizeUserText(msg.getContent())));
            } else if (msg.getRole() == MessageRole.CHATBOT) {
                msgs.add(GroqRequest.Message.assistant(msg.getContent()));
            }
        }

        // 3. 새 사용자 메시지 (이미 sanitize됨)
        msgs.add(GroqRequest.Message.user(latestSanitizedUserText));

        // 4. History truncation: system prompt + last N messages
        return truncateMessages(msgs, config.getMaxHistoryMessages());
    }

    /**
     * Phase 2 의뢰서용 messages[] 배열 구성.
     */
    private List<GroqRequest.Message> buildBriefMessages(Consultation consultation) {
        List<GroqRequest.Message> msgs = new ArrayList<>();

        // 1. 의뢰서 전용 시스템 프롬프트
        String systemPrompt = promptService.loadRouterBriefPrompt();
        msgs.add(GroqRequest.Message.system(systemPrompt));

        // 2. 전체 대화 내역
        List<Message> history = messageReader.findAllByConsultationId(consultation.getId());
        for (Message msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                msgs.add(GroqRequest.Message.user(
                        sanitizeService.sanitizeUserText(msg.getContent())));
            } else if (msg.getRole() == MessageRole.CHATBOT) {
                msgs.add(GroqRequest.Message.assistant(msg.getContent()));
            }
        }

        return truncateMessages(msgs, config.getMaxHistoryMessages());
    }

    /**
     * History truncation: 시스템 프롬프트(첫 메시지) + 최근 maxMessages개 유지.
     */
    private List<GroqRequest.Message> truncateMessages(List<GroqRequest.Message> messages, int maxMessages) {
        if (messages.size() <= maxMessages + 1) {
            return messages;
        }
        List<GroqRequest.Message> truncated = new ArrayList<>();
        truncated.add(messages.get(0)); // system prompt
        truncated.addAll(messages.subList(messages.size() - maxMessages, messages.size()));
        return truncated;
    }
}
