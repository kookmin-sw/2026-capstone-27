package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Groq Chat Completions API 요청 DTO.
 * POST https://api.groq.com/openai/v1/chat/completions
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroqRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;

    /**
     * messages[] 배열 내 개별 메시지.
     * {role: "system"|"assistant"|"user", content: "..."}
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
     */
    public static GroqRequest forChat(String model, List<Message> messages) {
        return GroqRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.3)
                .maxCompletionTokens(1024)
                .topP(0.9)
                .build();
    }

    /**
     * Phase 2 의뢰서 생성 (json_object 모드).
     * llama-3.3-70b-versatile는 json_schema를 지원하지 않으므로 json_object 모드 사용.
     */
    public static GroqRequest forBrief(String model, List<Message> messages) {
        return GroqRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.5)
                .maxCompletionTokens(4096)
                .topP(0.95)
                .responseFormat(Map.of("type", "json_object"))
                .build();
    }
}
