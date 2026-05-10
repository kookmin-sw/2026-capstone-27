package org.example.shield.ai.dto;

/**
 * AI API 호출 결과 래퍼.
 * responseId: Cohere chat completion ID (감사 로깅용).
 */
public record AiCallResult<T>(
        String responseId,
        T data,
        Integer tokensInput,
        Integer tokensOutput,
        Integer latencyMs
) {
}
