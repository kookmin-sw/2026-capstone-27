package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Phase 1 대화 응답 파싱 결과.
 * Grok output[0].content[0].text를 JSON 파싱한 결과.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatParsedResponse {

    private String nextQuestion;
    private List<String> primaryField;  // null | ["DEPOSIT_FRAUD"]
    private List<String> tags;          // null | ["부동산", "보증금 분쟁"]
    private boolean allCompleted;
}
