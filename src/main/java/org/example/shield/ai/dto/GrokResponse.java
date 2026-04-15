package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Grok Responses API 응답 DTO.
 * POST https://api.x.ai/v1/responses → 응답 구조
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrokResponse {

    private String id;                  // response ID (Stateful 연결용)
    private List<OutputItem> output;
    private Usage usage;

    /**
     * 응답 텍스트 추출: output[0].content[0].text
     */
    public String extractContent() {
        if (output == null || output.isEmpty()) {
            return null;
        }
        OutputItem first = output.get(0);
        if (first.getContent() == null || first.getContent().isEmpty()) {
            return null;
        }
        return first.getContent().get(0).getText();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutputItem {
        private String type;
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
    public static class Usage {
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer cachedTokens;
        private Integer totalTokens;
    }
}
