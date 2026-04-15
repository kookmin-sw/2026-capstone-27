package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.GrokApiConfig;
import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.ai.dto.GrokRequest;
import org.example.shield.ai.infrastructure.GuardrailFilter;
import org.example.shield.ai.infrastructure.SanitizeService;
import org.example.shield.common.enums.MessageRole;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Grok AI 서비스 — AiClient를 활용한 AI 기능.
 *
 * ChatService / AnalysisService에서 호출.
 * - chat(): 대화 API (Phase 1)
 * - generateBrief(): 의뢰서 생성 API (Phase 2)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrokService {

    private final AiClient aiClient;
    private final GrokApiConfig config;
    private final PromptService promptService;
    private final SanitizeService sanitizeService;
    private final GuardrailFilter guardrailFilter;
    private final MessageReader messageReader;

    /**
     * Phase 1 대화 — 사용자 메시지 처리 후 AI 응답 반환.
     *
     * @param consultation 상담 엔티티
     * @param sanitizedUserText 사용자 입력 (sanitize 완료)
     * @return GrokCallResult<ChatParsedResponse>
     */
    public GrokCallResult<ChatParsedResponse> chat(Consultation consultation, String sanitizedUserText) {
        String consultationId = consultation.getId().toString();
        String lastResponseId = consultation.getLastResponseId();

        GrokCallResult<ChatParsedResponse> result;

        if (lastResponseId != null) {
            // Stateful 모드: 새 메시지만 전달
            result = aiClient.callChatFollowUp(
                    config.getChatModel(),
                    sanitizedUserText,
                    lastResponseId,
                    consultationId
            );
        } else {
            // Stateless 모드: 전체 대화를 input[] 배열로 구성
            List<GrokRequest.InputItem> inputArray = buildChatInputArray(
                    consultation, sanitizedUserText);
            result = aiClient.callChatInitial(
                    config.getChatModel(),
                    inputArray,
                    consultationId
            );
        }

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
        List<GrokRequest.InputItem> inputArray = buildBriefInputArray(consultation);
        GrokCallResult<BriefParsedResponse> result = aiClient.callBrief(
                config.getBriefModel(), inputArray);

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
     * Phase 1 대화용 input[] 배열 구성 (Stateless 모드).
     * system + assistant/user 턴을 역할 배열로 구조화 (P0-III).
     */
    private List<GrokRequest.InputItem> buildChatInputArray(
            Consultation consultation, String latestSanitizedUserText) {

        List<GrokRequest.InputItem> items = new ArrayList<>();

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

        items.add(GrokRequest.InputItem.system(systemPrompt));

        // 2. 기존 대화 내역 (시간순)
        List<Message> messages = messageReader.findAllByConsultationId(consultation.getId());
        for (Message msg : messages) {
            if (msg.getRole() == MessageRole.USER) {
                // 기존 사용자 메시지도 sanitize 적용
                items.add(GrokRequest.InputItem.user(
                        sanitizeService.sanitizeUserText(msg.getContent())));
            } else if (msg.getRole() == MessageRole.CHATBOT) {
                items.add(GrokRequest.InputItem.assistant(msg.getContent()));
            }
        }

        // 3. 새 사용자 메시지 (이미 sanitize됨)
        items.add(GrokRequest.InputItem.user(latestSanitizedUserText));

        return items;
    }

    /**
     * Phase 2 의뢰서용 input[] 배열 구성.
     */
    private List<GrokRequest.InputItem> buildBriefInputArray(Consultation consultation) {
        List<GrokRequest.InputItem> items = new ArrayList<>();

        // 1. 의뢰서 전용 시스템 프롬프트
        String systemPrompt = promptService.loadRouterBriefPrompt();
        items.add(GrokRequest.InputItem.system(systemPrompt));

        // 2. 전체 대화 내역
        List<Message> messages = messageReader.findAllByConsultationId(consultation.getId());
        for (Message msg : messages) {
            if (msg.getRole() == MessageRole.USER) {
                items.add(GrokRequest.InputItem.user(
                        sanitizeService.sanitizeUserText(msg.getContent())));
            } else if (msg.getRole() == MessageRole.CHATBOT) {
                items.add(GrokRequest.InputItem.assistant(msg.getContent()));
            }
        }

        return items;
    }
}
