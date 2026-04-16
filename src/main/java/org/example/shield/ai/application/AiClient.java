package org.example.shield.ai.application;

import org.example.shield.ai.dto.BriefParsedResponse;
import org.example.shield.ai.dto.ChatParsedResponse;
import org.example.shield.ai.dto.GrokCallResult;
import org.example.shield.ai.dto.GroqRequest;

import java.util.List;

/**
 * LLM 호출 추상화 인터페이스.
 * 현재 구현체: GroqClient (Groq Chat Completions API).
 */
public interface AiClient {

    /**
     * Phase 1 대화 — 전체 chatHistory를 messages[]로 전달.
     */
    GrokCallResult<ChatParsedResponse> callChat(
            String model, List<GroqRequest.Message> messages);

    /**
     * Phase 2 의뢰서 생성 (json_object 모드).
     */
    GrokCallResult<BriefParsedResponse> callBrief(
            String model, List<GroqRequest.Message> messages);
}
