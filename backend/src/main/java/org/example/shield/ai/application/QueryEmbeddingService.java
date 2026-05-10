package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.infrastructure.CohereClient;
import org.example.shield.ai.infrastructure.RagMetrics;
import org.springframework.stereotype.Service;

/**
 * 쿼리 임베딩 생성 서비스.
 *
 * <p>{@link CohereClient}를 직접 호출하여 임베딩을 생성한다. 캐시 레이어는
 * Issue #38에서 제거되어 매 호출마다 Cohere Embed API를 호출한다.</p>
 *
 * <p>Cohere 호출 지연·성공률은 {@link RagMetrics#timeCohereEmbed(java.util.function.Supplier)}
 * 타이머로 계측된다. 호출 실패 시 예외는 상위(PgLegalRetrievalService)로 전파되어
 * 영벡터 degrade 경로로 이어진다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryEmbeddingService {

    private final CohereClient cohereClient;
    private final CohereApiConfig cohereConfig;
    private final RagMetrics ragMetrics;

    /**
     * 쿼리 임베딩을 생성해 반환한다.
     *
     * @param query 검색 쿼리
     * @return 임베딩 벡터 (Cohere 호출 실패 시 RuntimeException 상위 전파)
     */
    public float[] embedQuery(String query) {
        String model = cohereConfig.getEmbedModel();
        return ragMetrics.timeCohereEmbed(() -> cohereClient.embedQuery(model, query));
    }
}
