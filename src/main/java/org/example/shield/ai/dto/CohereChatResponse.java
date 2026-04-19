package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Cohere Chat API v2 응답 DTO.
 * POST https://api.cohere.com/v2/chat 의 응답 구조.
 *
 * 응답 예시:
 * {
 *   "id": "5a50480a-cf52-46f0-af01-53d18539bd31",
 *   "message": {
 *     "role": "assistant",
 *     "content": [{"type": "text", "text": "..."}]
 *   },
 *   "finish_reason": "COMPLETE",
 *   "meta": {
 *     "billed_units": {"input_tokens": 17, "output_tokens": 12},
 *     "tokens": {"input_tokens": 215, "output_tokens": 12}
 *   }
 * }
 *
 * finish_reason 값: COMPLETE, STOP_SEQUENCE, MAX_TOKENS, TOOL_CALL, ERROR, TIMEOUT
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CohereChatResponse {

    private String id;
    private AssistantMessage message;

    @JsonProperty("finish_reason")
    private String finishReason;

    private Meta meta;

    /**
     * 응답 텍스트 추출: message.content[0].text
     * content는 배열이며 여러 type(text/tool_call 등)이 올 수 있으나
     * 현재 파이프라인은 type="text"만 사용.
     */
    public String extractContent() {
        if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
            return null;
        }
        for (ContentPart part : message.getContent()) {
            if ("text".equalsIgnoreCase(part.getType()) && part.getText() != null) {
                return part.getText();
            }
        }
        // fallback — type 필드 없이 text만 있는 경우
        ContentPart first = message.getContent().get(0);
        return first.getText();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssistantMessage {
        private String role;
        private List<ContentPart> content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentPart {
        private String type;
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        /**
         * 사용자 청구 기준 토큰.
         * RAG 검색 등 서버 측 추가 호출이 있을 때 meta.tokens와 달라질 수 있음.
         */
        @JsonProperty("billed_units")
        private TokenUsage billedUnits;

        /**
         * 실제 모델 투입/출력 토큰 (pre-billing).
         */
        private TokenUsage tokens;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenUsage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;
    }
}
