package org.example.shield.ai.application;

import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.AiCallResult;
import org.example.shield.ai.dto.CohereChatRequest;

import java.util.List;

/**
 * LLM 호출 추상화 인터페이스.
 * 현재 구현체: CohereClient (Cohere Chat API v2).
 */
public interface AiClient {

    /**
     * Phase 1 대화 — 전체 chatHistory를 messages[]로 전달.
     */
    AiCallResult<ChatParsedResponse> callChat(
            String model, List<CohereChatRequest.Message> messages);

    /**
     * Phase 2 의뢰서 생성 (json_object 모드).
     */
    AiCallResult<BriefParsedResponse> callBrief(
            String model, List<CohereChatRequest.Message> messages);
}
