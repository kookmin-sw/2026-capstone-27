package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * {@code src/main/resources/seed/cases/*.json}의 바인딩 DTO.
 *
 * <p>{@code scripts/fetch_cases.py}가 국가법령정보 OpenAPI에서 판례 본문을 수집해 저장한
 * 시드 파일 포맷. 판례 1건 = 파일 1개 = {@link LegalCaseSeed} 1개. C-4 인제스트 파이프라인이
 * 읽어 들여 Cohere embed → legal_cases 테이블 upsert한다.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LegalCaseSeed(
        Meta meta,
        @JsonProperty("case") Case caseData
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            String source,
            @JsonProperty("source_id") String sourceId,
            @JsonProperty("source_url") String sourceUrl,
            @JsonProperty("fetched_at") String fetchedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Case(
            @JsonProperty("case_no") String caseNo,
            String court,
            @JsonProperty("case_name") String caseName,
            @JsonProperty("decision_date") String decisionDate,
            @JsonProperty("case_type") String caseType,
            @JsonProperty("judgment_type") String judgmentType,
            String disposition,
            String headnote,
            String holding,
            String reasoning,
            @JsonProperty("full_text") String fullText,
            @JsonProperty("cited_articles") List<String> citedArticles,
            @JsonProperty("cited_cases") List<String> citedCases,
            @JsonProperty("category_ids") List<String> categoryIds
    ) {
    }
}
