package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * {@code src/main/resources/seed/civil-law.json}의 바인딩 DTO.
 *
 * <p>{@code scripts/fetch_civil_law.py}가 국가법령정보 OpenAPI에서 수집해 저장한
 * 민법 전체 조문 시드 데이터. B-2 인제스트 파이프라인이 읽어 들인다.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CivilLawSeed(
        Meta meta,
        List<Article> articles
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            @JsonProperty("law_id") String lawId,
            @JsonProperty("law_name") String lawName,
            String mst,
            @JsonProperty("effective_date") String effectiveDate,
            @JsonProperty("source_url") String sourceUrl,
            @JsonProperty("fetched_at") String fetchedAt,
            @JsonProperty("total_articles") Integer totalArticles
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Article(
            @JsonProperty("law_id") String lawId,
            @JsonProperty("law_name") String lawName,
            @JsonProperty("article_no") String articleNo,
            @JsonProperty("article_no_int") Integer articleNoInt,
            @JsonProperty("article_title") String articleTitle,
            String content,
            String book,
            String chapter,
            String section,
            @JsonProperty("effective_date") String effectiveDate,
            @JsonProperty("source_mst") String sourceMst,
            @JsonProperty("source_law_id") String sourceLawId
    ) {
    }
}
