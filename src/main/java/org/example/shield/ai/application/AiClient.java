package org.example.shield.ai.application;

import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.ai.dto.GrokRequest;

import java.util.List;

/**
 * LLM 호출 추상화 인터페이스.
 * 현재 구현체: GrokClient (xAI Grok Responses API).
 * 향후 Tier B-1에서 AnthropicClaudeClient 등 추가 가능.
 */
public interface AiClient {

    /**
     * Phase 1 대화 - 첫 호출 (Stateless).
     * 시스템 프롬프트 + 전체 chatHistory를 input[]로 전달.
     */
    GrokCallResult<ChatParsedResponse> callChatInitial(
            String model, List<GrokRequest.InputItem> inputArray, String consultationId);

    /**
     * Phase 1 대화 - 후속 호출 (Stateful).
     * 새 사용자 메시지만 전달 + previous_response_id로 맥락 연결.
     */
    GrokCallResult<ChatParsedResponse> callChatFollowUp(
            String model, String userMessage, String previousResponseId, String consultationId);

    /**
     * Phase 2 의뢰서 생성 (항상 Stateless + Structured Outputs).
     */
    GrokCallResult<BriefParsedResponse> callBrief(
            String model, List<GrokRequest.InputItem> inputArray);
}
