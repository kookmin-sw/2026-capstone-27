package org.example.shield.ai.application;

import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.infrastructure.CohereClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QueryEmbeddingService}의 Cache-aside 동작 검증 (Phase B-5).
 *
 * <p>핵심 동작:</p>
 * <ul>
 *   <li>캐시 HIT → Cohere 호출 생략</li>
 *   <li>캐시 MISS → Cohere 호출 + 캐시에 PUT</li>
 *   <li>Cohere 실패 시 예외는 그대로 상위 전파 (PgLegalRetrievalService에서 degrade)</li>
 * </ul>
 */
class QueryEmbeddingServiceTest {

    private CohereClient cohereClient;
    private CohereApiConfig cohereConfig;
    private EmbeddingCache embeddingCache;
    private QueryEmbeddingService service;

    @BeforeEach
    void setUp() {
        cohereClient = mock(CohereClient.class);
        cohereConfig = mock(CohereApiConfig.class);
        embeddingCache = mock(EmbeddingCache.class);
        when(cohereConfig.getEmbedModel()).thenReturn("embed-v4.0");
        service = new QueryEmbeddingService(cohereClient, cohereConfig, embeddingCache);
    }

    @Test
    @DisplayName("캐시 HIT — Cohere 호출 없이 캐시 값 반환")
    void cacheHit_skipsCohereCall() {
        float[] cached = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingCache.get(eq("embed-v4.0"), eq("전세 보증금"))).thenReturn(Optional.of(cached));

        float[] result = service.embedQuery("전세 보증금");

        assertThat(result).isSameAs(cached);
        verify(cohereClient, never()).embedQuery(anyString(), anyString());
        verify(embeddingCache, never()).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("캐시 MISS — Cohere 호출 후 캐시에 저장")
    void cacheMiss_callsCohereAndStores() {
        float[] computed = new float[]{0.5f, -0.1f};
        when(embeddingCache.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(cohereClient.embedQuery(eq("embed-v4.0"), eq("임대차 해지"))).thenReturn(computed);

        float[] result = service.embedQuery("임대차 해지");

        assertThat(result).isSameAs(computed);
        verify(cohereClient, times(1)).embedQuery("embed-v4.0", "임대차 해지");
        verify(embeddingCache, times(1)).put("embed-v4.0", "임대차 해지", computed);
    }

    @Test
    @DisplayName("Cohere 실패 — 예외 상위 전파 + 캐시 저장 안 함")
    void cohereFailure_propagatesWithoutCaching() {
        when(embeddingCache.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(cohereClient.embedQuery(anyString(), anyString()))
                .thenThrow(new RuntimeException("Cohere down"));

        try {
            service.embedQuery("소유권 이전");
        } catch (RuntimeException expected) {
            assertThat(expected).hasMessage("Cohere down");
        }

        verify(embeddingCache, never()).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("빈 임베딩 응답 — 캐시 저장 생략")
    void emptyEmbedding_skipsCaching() {
        when(embeddingCache.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(cohereClient.embedQuery(anyString(), anyString())).thenReturn(new float[0]);

        float[] result = service.embedQuery("무언가");

        assertThat(result).isEmpty();
        verify(embeddingCache, never()).put(anyString(), anyString(), any());
    }
}
