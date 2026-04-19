package org.example.shield.ai.application;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.infrastructure.CohereClient;
import org.example.shield.ai.infrastructure.RagMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QueryEmbeddingService}의 Cohere 직접 호출 동작 검증.
 *
 * <p>Issue #38에서 임베딩 캐시 레이어를 제거하여 매 호출마다 Cohere를 호출한다.
 * 본 테스트는 (1) Cohere 직접 위임, (2) 예외 상위 전파,
 * (3) 메트릭 타이머 수집을 검증한다.</p>
 */
class QueryEmbeddingServiceTest {

    private CohereClient cohereClient;
    private CohereApiConfig cohereConfig;
    private SimpleMeterRegistry meterRegistry;
    private RagMetrics ragMetrics;
    private QueryEmbeddingService service;

    @BeforeEach
    void setUp() {
        cohereClient = mock(CohereClient.class);
        cohereConfig = mock(CohereApiConfig.class);
        meterRegistry = new SimpleMeterRegistry();
        ragMetrics = new RagMetrics(meterRegistry);
        when(cohereConfig.getEmbedModel()).thenReturn("embed-v4.0");
        service = new QueryEmbeddingService(cohereClient, cohereConfig, ragMetrics);
    }

    @Test
    @DisplayName("Cohere 호출 위임 — 결과 그대로 반환")
    void delegatesToCohereAndReturnsResult() {
        float[] computed = new float[]{0.5f, -0.1f};
        when(cohereClient.embedQuery(eq("embed-v4.0"), eq("임대차 해지"))).thenReturn(computed);

        float[] result = service.embedQuery("임대차 해지");

        assertThat(result).isSameAs(computed);
        verify(cohereClient, times(1)).embedQuery("embed-v4.0", "임대차 해지");
    }

    @Test
    @DisplayName("Cohere 실패 — 예외 상위 전파")
    void cohereFailure_propagates() {
        when(cohereClient.embedQuery(anyString(), anyString()))
                .thenThrow(new RuntimeException("Cohere down"));

        try {
            service.embedQuery("소유권 이전");
        } catch (RuntimeException expected) {
            assertThat(expected).hasMessage("Cohere down");
        }
    }

    @Test
    @DisplayName("빈 임베딩 응답 — 빈 배열 그대로 반환")
    void emptyEmbedding_returnsEmpty() {
        when(cohereClient.embedQuery(anyString(), anyString())).thenReturn(new float[0]);

        float[] result = service.embedQuery("무언가");

        assertThat(result).isEmpty();
    }

    // === 메트릭 수집 확인 ===

    @Test
    @DisplayName("메트릭 — Cohere 성공 시 success 타이머 기록")
    void metrics_cohereSuccessRecordsSuccessTimer() {
        when(cohereClient.embedQuery(anyString(), anyString())).thenReturn(new float[]{0.1f});

        service.embedQuery("전세");

        assertThat(meterRegistry.timer(RagMetrics.METRIC_COHERE_EMBED, "outcome", "success").count())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("메트릭 — Cohere 실패 시 failure 타이머 기록")
    void metrics_cohereFailureRecordsFailureTimer() {
        when(cohereClient.embedQuery(anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        try {
            service.embedQuery("전세");
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(meterRegistry.timer(RagMetrics.METRIC_COHERE_EMBED, "outcome", "failure").count())
                .isEqualTo(1L);
    }
}
