package org.example.shield.ai.dto;

/**
 * Grok API 호출 결과 래퍼.
 * responseId (Stateful 연결용) + 파싱 결과를 함께 전달.
 */
public record GrokCallResult<T>(
        String responseId,
        T data,
        Integer tokensInput,
        Integer tokensOutput,
        Integer latencyMs
) {
}
