package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Phase 2 의뢰서 응답 파싱 결과.
 * Structured Outputs으로 모델이 반환하는 JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BriefParsedResponse {

    private String title;
    private String content;
    private List<KeyIssue> keyIssues;
    private List<String> keywords;
    private String strategy;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyIssue {
        private String title;
        private String description;
    }
}
