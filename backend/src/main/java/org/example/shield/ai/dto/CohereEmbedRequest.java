package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Cohere Embed API v2 요청 DTO.
 * POST https://api.cohere.com/v2/embed
 *
 * 필수 필드: model, texts, input_type, embedding_types
 * 기본값: input_type="search_document" (법령 조문 적재용), embedding_types=["float"]
 *
 * input_type 값:
 * - "search_document": 검색 대상 문서(법령 조문) 임베딩
 * - "search_query": 검색 질의 임베딩
 * - "classification" / "clustering": 분류/군집용
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CohereEmbedRequest {

    private final String model;

    private final List<String> texts;

    @JsonProperty("input_type")
    private final String inputType;

    @JsonProperty("embedding_types")
    private final List<String> embeddingTypes;

    /**
     * embed-v4.0 기본 출력 차원은 1536. Matryoshka 구조로 256/512/1024/1536 선택 가능.
     * SHIELD는 1024 사용(Flyway V4의 vector(1024) 컬럼과 정합).
     */
    @JsonProperty("output_dimension")
    private final Integer outputDimension;

    /**
     * 문서(조문) 임베딩 생성용 팩토리. B-2 인제스트 파이프라인이 사용.
     */
    public static CohereEmbedRequest forDocument(String model, List<String> texts, int outputDimension) {
        return CohereEmbedRequest.builder()
                .model(model)
                .texts(texts)
                .inputType("search_document")
                .embeddingTypes(List.of("float"))
                .outputDimension(outputDimension)
                .build();
    }

    /**
     * 쿼리 임베딩 생성용 팩토리. Layer 2 검색 시점에 사용.
     */
    public static CohereEmbedRequest forQuery(String model, String query, int outputDimension) {
        return CohereEmbedRequest.builder()
                .model(model)
                .texts(List.of(query))
                .inputType("search_query")
                .embeddingTypes(List.of("float"))
                .outputDimension(outputDimension)
                .build();
    }
}
