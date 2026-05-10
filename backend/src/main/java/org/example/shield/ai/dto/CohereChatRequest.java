package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Cohere Chat API v2 요청 DTO.
 * POST https://api.cohere.com/v2/chat
 *
 * v2 주요 필드:
 * - model: command-a-03-2025 등
 * - messages: [{role, content}]  역할은 lowercase (system/user/assistant/tool)
 * - temperature: 0.0~1.0 (기본 0.3)
 * - max_tokens: 응답 최대 토큰 수
 * - p: top-p (기본 0.75)
 * - response_format: {type: "json_object"} — JSON 응답 강제 (schema 선택적 지원)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohereChatRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double p;

    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;

    /**
     * messages[] 배열 내 개별 메시지.
     * v2에서 role은 lowercase: "system" | "user" | "assistant" | "tool"
     */
    @Getter
    @Builder
    public static class Message {
        private String role;
        private String content;

        public static Message system(String text) {
            return Message.builder()
                    .role("system")
                    .content(text)
                    .build();
        }

        public static Message assistant(String text) {
            return Message.builder()
                    .role("assistant")
                    .content(text)
                    .build();
        }

        public static Message user(String text) {
            return Message.builder()
                    .role("user")
                    .content(text)
                    .build();
        }
    }

    // --- Factory Methods ---

    /**
     * Phase 1 대화: 전체 chatHistory를 messages[]로 전달.
     * response_format=json_object 로 모델 출력을 JSON 객체로 강제 (Issue #56).
     */
    public static CohereChatRequest forChat(String model, List<Message> messages) {
        return CohereChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.3)
                .maxTokens(1024)
                .p(0.9)
                .responseFormat(Map.of("type", "json_object"))
                .build();
    }

    /**
     * Phase 2 의뢰서 생성 (json_object 모드).
     * Cohere v2는 response_format={type: "json_object"}를 모든 command 계열에서 지원.
     */
    public static CohereChatRequest forBrief(String model, List<Message> messages) {
        return CohereChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.5)
                .maxTokens(4096)
                .p(0.95)
                .responseFormat(Map.of("type", "json_object"))
                .build();
    }

    /**
     * RAG Layer 1 의도 분류 (json_object 모드, 저온도).
     * temperature 0.1로 결정적 출력, max_tokens 512로 경량 호출.
     */
    public static CohereChatRequest forClassify(String model, List<Message> messages) {
        return CohereChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.1)
                .maxTokens(512)
                .responseFormat(Map.of("type", "json_object"))
                .build();
    }
}
