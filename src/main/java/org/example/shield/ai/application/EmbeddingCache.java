package org.example.shield.ai.application;

import java.util.Optional;

/**
 * 쿼리 임베딩 캐시 추상화 (Phase B-5).
 *
 * <p>동일한 검색 쿼리가 짧은 시간 내에 반복적으로 들어올 때 Cohere Embed API 재호출을
 * 피해 지연과 비용을 줄인다. 구현체는 Redis(운영) 또는 Noop(개발/테스트)을 사용한다.</p>
 *
 * <p>키 설계는 구현체에 위임하지만 일반적으로 {@code model}과 {@code query}를 함께
 * 해싱하여 충돌과 모델 교체 혼선을 방지한다.</p>
 */
public interface EmbeddingCache {

    /**
     * 캐시 조회. 히트 시 저장된 임베딩을 반환한다.
     *
     * @param model 임베딩 모델 ID (예: "embed-v4.0")
     * @param query 원본 쿼리 텍스트 (정규화 전)
     * @return 캐시된 임베딩 (없으면 {@link Optional#empty()})
     */
    Optional<float[]> get(String model, String query);

    /**
     * 캐시 저장.
     *
     * @param model 임베딩 모델 ID
     * @param query 원본 쿼리 텍스트
     * @param embedding 저장할 임베딩 (null/빈 배열은 저장하지 않는다)
     */
    void put(String model, String query, float[] embedding);
}
