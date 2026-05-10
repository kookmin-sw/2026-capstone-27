package org.example.shield.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Cohere Embed API v2 응답 DTO.
 * POST https://api.cohere.com/v2/embed
 *
 * 응답 구조:
 * {
 *   "id": "...",
 *   "embeddings": { "float": [[0.1, 0.2, ...], ...] },
 *   "texts": ["input1", "input2"],
 *   "meta": { "api_version": {...}, "billed_units": { "input_tokens": N } }
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CohereEmbedResponse {

    private String id;

    /**
     * 요청 시 embedding_types=["float"]이면 {@code embeddings.float}에 {@code List<List<Double>>}가 담긴다.
     * Cohere v2는 다양한 타입(int8, uint8, binary, ubinary, float) 동시 요청을 지원하므로 래퍼 구조.
     */
    private Embeddings embeddings;

    private List<String> texts;

    private Meta meta;

    /**
     * 첫 번째(또는 유일한) float 임베딩을 float[]로 반환.
     * 단일 문서 쿼리 임베딩 추출용.
     */
    public float[] extractFirstFloatVector() {
        if (embeddings == null || embeddings.getFloatVectors() == null
                || embeddings.getFloatVectors().isEmpty()) {
            return null;
        }
        return toFloatArray(embeddings.getFloatVectors().get(0));
    }

    /**
     * 모든 float 임베딩을 float[] 리스트로 반환. 배치 인제스트용.
     */
    public List<float[]> extractAllFloatVectors() {
        if (embeddings == null || embeddings.getFloatVectors() == null) {
            return List.of();
        }
        return embeddings.getFloatVectors().stream()
                .map(CohereEmbedResponse::toFloatArray)
                .toList();
    }

    private static float[] toFloatArray(List<Double> src) {
        if (src == null) return null;
        float[] out = new float[src.size()];
        for (int i = 0; i < src.size(); i++) {
            out[i] = src.get(i).floatValue();
        }
        return out;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embeddings {
        @JsonProperty("float")
        private List<List<Double>> floatVectors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("billed_units")
        private BilledUnits billedUnits;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BilledUnits {
        @JsonProperty("input_tokens")
        private Integer inputTokens;
    }
}
