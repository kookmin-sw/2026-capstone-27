package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.infrastructure.CohereClient;
import org.example.shield.ai.infrastructure.RagMetrics;
import org.springframework.stereotype.Service;

/**
 * 쿼리 임베딩을 생성하는 Cache-aside 서비스 (Phase B-5).
 *
 * <p>흐름:</p>
 * <ol>
 *   <li>{@link EmbeddingCache#get(String, String)} 조회 — hit이면 즉시 반환</li>
 *   <li>miss → {@link CohereClient#embedQuery(String, String)} 호출</li>
 *   <li>성공 시 캐시에 저장 (TTL은 캐시 구현이 관리)</li>
 * </ol>
 *
 * <p>캐시 장애나 비활성화 상태에서도 Cohere 호출 경로가 그대로 유지되므로
 * 기능 가용성에는 영향이 없다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryEmbeddingService {

    private final CohereClient cohereClient;
    private final CohereApiConfig cohereConfig;
    private final EmbeddingCache embeddingCache;
    private final RagMetrics ragMetrics;

    /**
     * 쿼리 임베딩을 반환한다. 캐시 히트 시 Cohere 호출을 생략한다.
     *
     * @param query 검색 쿼리 (공백 trim 후 캐시 키 생성)
     * @return 임베딩 벡터 (Cohere 호출 실패 시 RuntimeException 상위 전파)
     */
    public float[] embedQuery(String query) {
        String model = cohereConfig.getEmbedModel();
        var cached = embeddingCache.get(model, query);
        if (cached.isPresent()) {
            log.debug("쿼리 임베딩 캐시 HIT: model={}, queryLen={}", model, query.length());
            ragMetrics.recordCacheHit();
            return cached.get();
        }
        ragMetrics.recordCacheMiss();
        float[] vec = ragMetrics.timeCohereEmbed(() -> cohereClient.embedQuery(model, query));
        if (vec != null && vec.length > 0) {
            embeddingCache.put(model, query, vec);
        }
        return vec;
    }
}
