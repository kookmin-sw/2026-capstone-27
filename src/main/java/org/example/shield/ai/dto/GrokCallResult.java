package org.example.shield.ai.dto;

/**
 * AI API 호출 결과 래퍼.
 * responseId: Groq completion ID (감사 로깅용, Stateful 연결 용도 아님).
 */
public record GrokCallResult<T>(
        String responseId,
        T data,
        Integer tokensInput,
        Integer tokensOutput,
        Integer latencyMs
) {
}
