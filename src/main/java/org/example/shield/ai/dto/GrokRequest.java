package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Grok Responses API 요청 DTO.
 * POST https://api.x.ai/v1/responses
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrokRequest {

    private String model;
    private List<InputItem> input;
    private Double temperature;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("previous_response_id")
    private String previousResponseId;

    private Boolean store;

    @JsonProperty("prompt_cache_key")
    private String promptCacheKey;

    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;

    /**
     * input[] 배열 내 개별 항목.
     * {role: "system"|"assistant"|"user", content: [{type: "text", text: "..."}]}
     */
    @Getter
    @Builder
    public static class InputItem {
        private String role;
        private List<ContentPart> content;

        public static InputItem system(String text) {
            return InputItem.builder()
                    .role("system")
                    .content(List.of(ContentPart.text(text)))
                    .build();
        }

        public static InputItem assistant(String text) {
            return InputItem.builder()
                    .role("assistant")
                    .content(List.of(ContentPart.text(text)))
                    .build();
        }

        public static InputItem user(String text) {
            return InputItem.builder()
                    .role("user")
                    .content(List.of(ContentPart.text(text)))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ContentPart {
        private String type;
        private String text;

        public static ContentPart text(String text) {
            return ContentPart.builder()
                    .type("text")
                    .text(text)
                    .build();
        }
    }

    // --- Factory Methods ---

    /**
     * Phase 1 첫 호출 (Stateless): 전체 chatHistory를 input[]로 전달.
     */
    public static GrokRequest forChatInitial(String model, List<InputItem> inputArray,
                                              String consultationId) {
        return GrokRequest.builder()
                .model(model)
                .input(inputArray)
                .temperature(0.3)
                .maxOutputTokens(1024)
                .topP(0.9)
                .store(true)
                .promptCacheKey(consultationId)
                .build();
    }

    /**
     * Phase 1 후속 호출 (Stateful): 새 메시지만 전달 + previous_response_id.
     */
    public static GrokRequest forChatFollowUp(String model, String userMessage,
                                               String previousResponseId,
                                               String consultationId) {
        return GrokRequest.builder()
                .model(model)
                .input(List.of(InputItem.user(userMessage)))
                .temperature(0.3)
                .maxOutputTokens(1024)
                .topP(0.9)
                .previousResponseId(previousResponseId)
                .store(true)
                .promptCacheKey(consultationId)
                .build();
    }

    /**
     * Phase 2 의뢰서 생성 (항상 Stateless + Structured Outputs).
     */
    public static GrokRequest forBrief(String model, List<InputItem> inputArray) {
        return GrokRequest.builder()
                .model(model)
                .input(inputArray)
                .temperature(0.5)
                .maxOutputTokens(4096)
                .topP(0.95)
                .store(true)
                .responseFormat(briefResponseFormat())
                .build();
    }

    private static Map<String, Object> briefResponseFormat() {
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "BriefResponse",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of("type", "string"),
                                        "content", Map.of("type", "string"),
                                        "keyIssues", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "title", Map.of("type", "string"),
                                                                "description", Map.of("type", "string")
                                                        ),
                                                        "required", List.of("title", "description"),
                                                        "additionalProperties", false
                                                )
                                        ),
                                        "keywords", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string")
                                        ),
                                        "strategy", Map.of("type", "string")
                                ),
                                "required", List.of("title", "content", "keyIssues", "keywords", "strategy"),
                                "additionalProperties", false
                        )
                )
        );
    }
}
