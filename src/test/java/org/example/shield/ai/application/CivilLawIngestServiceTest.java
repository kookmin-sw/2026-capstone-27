package org.example.shield.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shield.ai.config.CivilLawCategoryMap;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.domain.LegalChunkEntity;
import org.example.shield.ai.dto.CivilLawSeed;
import org.example.shield.ai.infrastructure.CohereClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CivilLawIngestService} 단위 테스트.
 *
 * <p>실제 Cohere API/DB를 호출하지 않고, Mock으로 배치 임베딩 → upsert 경로를 검증.</p>
 */
class CivilLawIngestServiceTest {

    private CivilLawIngestService service;
    private CohereClient cohereClient;
    private CivilLawUpsertService upsertService;
    private CivilLawCategoryMap categoryMap;

    @BeforeEach
    void setUp() {
        cohereClient = mock(CohereClient.class);
        upsertService = mock(CivilLawUpsertService.class);

        categoryMap = new CivilLawCategoryMap();
        categoryMap.load();

        CohereApiConfig cohereConfig = new CohereApiConfig() {
            @Override public String getEmbedModel() { return "embed-v4.0"; }
            @Override public int getEmbedBatchSize() { return 96; }
            @Override public int getEmbedDimension() { return 1024; }
        };

        service = new CivilLawIngestService(
                new ObjectMapper(),
                cohereClient,
                cohereConfig,
                categoryMap,
                upsertService);
        // 테스트 속도를 위해 배치 지연 0
        ReflectionTestUtils.setField(service, "batchDelayMs", 0L);
    }

    @Test
    @DisplayName("dryRun: 임베딩 API/DB 호출 없음, 총 조문 수만 집계")
    void dryRunSkipsApiAndDb() {
        CivilLawIngestService.IngestSummary summary = service.run(true);

        assertThat(summary.totalArticles()).isGreaterThan(1000);
        assertThat(summary.totalUpserted()).isZero();
        assertThat(summary.totalFailed()).isZero();
        assertThat(summary.dryRun()).isTrue();

        verify(cohereClient, never()).embedDocuments(anyString(), anyList());
        verify(upsertService, never()).upsertBatch(anyList(), anyList(), anyString(), any());
    }

    @Test
    @DisplayName("정상 실행: 모든 배치 임베딩 성공 → 전 조문 upsert 호출")
    void ingestsAllArticles() {
        when(cohereClient.embedDocuments(eq("embed-v4.0"), anyList())).thenAnswer(inv -> {
            List<String> inputs = inv.getArgument(1);
            return inputs.stream().map(s -> new float[]{0.1f, 0.2f, 0.3f}).toList();
        });
        // upsertService는 요청된 만큼 그대로 upsert 처리했다고 응답
        when(upsertService.upsertBatch(anyList(), anyList(), eq("embed-v4.0"), any()))
                .thenAnswer(inv -> {
                    List<?> b = inv.getArgument(0);
                    return b.size();
                });

        CivilLawIngestService.IngestSummary summary = service.run(false);

        assertThat(summary.totalFailed()).isZero();
        assertThat(summary.dryRun()).isFalse();
        // 1193 조 / 96 = 12.4 → 13 배치
        assertThat(summary.totalBatches()).isEqualTo(13);
        assertThat(summary.totalUpserted()).isEqualTo(summary.totalArticles());

        verify(cohereClient, times(13)).embedDocuments(eq("embed-v4.0"), anyList());
        verify(upsertService, times(13)).upsertBatch(anyList(), anyList(), eq("embed-v4.0"), any());
    }

    @Test
    @DisplayName("엔티티 매핑 검증: 제303조 → 전세권 카테고리 포함")
    void entityMappingIncludesCategory() {
        when(cohereClient.embedDocuments(eq("embed-v4.0"), anyList())).thenAnswer(inv -> {
            List<String> inputs = inv.getArgument(1);
            return inputs.stream().map(s -> new float[]{0f}).toList();
        });

        // upsertService가 받는 EntityMaker를 실제로 호출해서 Entity 생성 결과를 캡처
        List<LegalChunkEntity> createdEntities = new ArrayList<>();
        when(upsertService.upsertBatch(anyList(), anyList(), anyString(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    List<CivilLawSeed.Article> batch = inv.getArgument(0);
                    @SuppressWarnings("unchecked")
                    List<float[]> vectors = inv.getArgument(1);
                    CivilLawUpsertService.EntityMaker maker = inv.getArgument(3);
                    for (int i = 0; i < batch.size(); i++) {
                        createdEntities.add(maker.apply(batch.get(i), vectors.get(i)));
                    }
                    return batch.size();
                });

        service.run(false);

        LegalChunkEntity art303 = createdEntities.stream()
                .filter(e -> "제303조".equals(e.getArticleNo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("제303조 엔티티 없음"));

        assertThat(art303.getLawName()).isEqualTo("민법");
        assertThat(art303.getLawId()).isEqualTo("law-civil");
        assertThat(art303.getCategoryIds())
                .anyMatch(t -> t.startsWith("chapter:제6장 전세권"))
                .contains("group:jeonse");
        assertThat(art303.getEffectiveDate()).isNotNull();
        assertThat(art303.getSourceUrl()).contains("MST=284415");
        assertThat(art303.getEmbeddingModel()).isEqualTo("embed-v4.0");
    }

    @Test
    @DisplayName("배치 실패 격리: 1개 배치 실패해도 다른 배치 계속 진행")
    void batchFailureIsolation() {
        AtomicInteger callCount = new AtomicInteger();
        when(cohereClient.embedDocuments(eq("embed-v4.0"), anyList())).thenAnswer(inv -> {
            int n = callCount.incrementAndGet();
            List<String> inputs = inv.getArgument(1);
            if (n == 3) {
                throw new RuntimeException("시뮬레이션 429 실패");
            }
            return inputs.stream().map(s -> new float[]{0f}).toList();
        });
        when(upsertService.upsertBatch(anyList(), anyList(), anyString(), any()))
                .thenAnswer(inv -> {
                    List<?> b = inv.getArgument(0);
                    return b.size();
                });

        CivilLawIngestService.IngestSummary summary = service.run(false);

        assertThat(summary.totalFailed()).isEqualTo(96);
        assertThat(summary.failedArticleNos()).hasSize(96);
        assertThat(summary.totalBatches()).isEqualTo(13);
        // 실패 1건 제외 나머지 12건의 upsert 호출
        verify(upsertService, times(12)).upsertBatch(anyList(), anyList(), anyString(), any());
    }
}
