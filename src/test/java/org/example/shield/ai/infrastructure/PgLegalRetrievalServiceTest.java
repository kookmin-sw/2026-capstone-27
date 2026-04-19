package org.example.shield.ai.infrastructure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.application.QueryEmbeddingService;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.domain.LegalChunkJpaRepository;
import org.example.shield.ai.domain.LegalChunkJpaRepository.LegalChunkRow;
import org.example.shield.ai.dto.LegalChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PgLegalRetrievalService}의 순수 로직을 단위 테스트로 검증.
 *
 * <p>실제 PostgreSQL/pgvector 쿼리 동작은 통합 테스트 범위로 분리하고,
 * 여기서는 벡터 변환/degrade 경로/카테고리 필터 정규화/쿼리 바인딩을 중심으로 확인한다.</p>
 */
class PgLegalRetrievalServiceTest {

    private LegalChunkJpaRepository repository;
    private QueryEmbeddingService queryEmbeddingService;
    private CohereApiConfig cohereConfig;
    private SimpleMeterRegistry meterRegistry;
    private RagMetrics ragMetrics;
    private PgLegalRetrievalService service;

    @BeforeEach
    void setUp() {
        repository = mock(LegalChunkJpaRepository.class);
        queryEmbeddingService = mock(QueryEmbeddingService.class);
        cohereConfig = mock(CohereApiConfig.class);
        meterRegistry = new SimpleMeterRegistry();
        ragMetrics = new RagMetrics(meterRegistry);
        when(cohereConfig.getEmbedModel()).thenReturn("embed-v4.0");
        when(cohereConfig.getEmbedDimension()).thenReturn(1024);
        service = new PgLegalRetrievalService(repository, queryEmbeddingService, cohereConfig,
                ragMetrics, 0.5, 0.3, 0.2, 40);
    }

    @Test
    @DisplayName("floatArrayToPgVector — pgvector 리터럴 포맷 검증")
    void toPgVector_format() {
        float[] v = new float[]{0.1f, -0.25f, 1.0f};
        String literal = PgLegalRetrievalService.floatArrayToPgVector(v);
        // 로케일 독립 소수점 + 대괄호로 감싼 CSV 형식
        assertThat(literal).startsWith("[").endsWith("]");
        assertThat(literal).contains("0.100000");
        assertThat(literal).contains("-0.250000");
        assertThat(literal).contains("1.000000");
        assertThat(literal.split(",")).hasSize(3);
    }

    @Test
    @DisplayName("retrieve — 벡터 쿼리 임베딩이 pgvector 리터럴로 바인딩되어 repository에 전달")
    void retrieve_bindsEmbeddingLiteral() {
        // given
        float[] qvec = new float[1024];
        qvec[0] = 0.5f;
        qvec[1023] = -0.3f;
        when(queryEmbeddingService.embedQuery(eq("전세 보증금 미반환")))
                .thenReturn(qvec);
        when(repository.search3Way(
                anyString(), anyString(), anyString(), nullable(String[].class),
                anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(row("민법", "제303조", "전세권의 내용", "전세권자는...", "2026-03-17", "url", 0.87)));

        // when
        List<LegalChunk> result = service.retrieve(
                "전세 보증금 미반환", List.of("전세", "보증금"), null, null, 3);

        // then: 벡터 리터럴 포맷 확인
        assertThat(result).hasSize(1);
        assertThat(result.get(0).articleNo()).isEqualTo("제303조");
        verify(repository).search3Way(
                argThat(lit -> lit.startsWith("[") && lit.endsWith("]")
                        && lit.contains("0.500000")),
                eq("전세 보증금 미반환"),
                contains("전세"),  // keywordQuery 는 '전세 | 보증금' 형태
                isNull(),            // categoryIds 없음
                eq(0.5), eq(0.3), eq(0.2),
                eq(3));
    }

    @Test
    @DisplayName("retrieve — 임베딩 실패 시 영벡터로 degrade (예외 전파 없음)")
    void retrieve_embeddingFailure_degradesToZeroVector() {
        when(queryEmbeddingService.embedQuery(anyString()))
                .thenThrow(new RuntimeException("Cohere down"));
        when(repository.search3Way(
                anyString(), anyString(), anyString(), nullable(String[].class),
                anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        List<LegalChunk> result = service.retrieve(
                "임대차 계약 해지", List.of("임대차"), null, null, 5);

        assertThat(result).isEmpty();
        // 영벡터(모든 0)가 바인딩되었는지 확인
        verify(repository).search3Way(
                argThat(lit -> lit.startsWith("[0,0,0")),
                anyString(), anyString(), nullable(String[].class),
                anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("retrieve — categoryIds가 주어지면 String[]로 정규화되어 repository에 전달")
    void retrieve_normalizesCategoryIds() {
        when(queryEmbeddingService.embedQuery(anyString()))
                .thenReturn(new float[1024]);
        when(repository.search3Way(
                anyString(), anyString(), anyString(), any(String[].class),
                anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        // null 원소를 포함해 정규화되는지 검증 — List.of는 null 불허, Arrays.asList 사용
        java.util.List<String> cats = java.util.Arrays.asList(
                "group:jeonse", "  chapter:제6장 전세권  ", "", null, "group:jeonse");
        service.retrieve(
                "전세금 반환",
                List.of("전세금"),
                cats,
                null,
                5);

        verify(repository).search3Way(
                anyString(), anyString(), anyString(),
                argThat(arr -> arr != null && arr.length == 2
                        && java.util.Arrays.asList(arr).contains("group:jeonse")
                        && java.util.Arrays.asList(arr).contains("chapter:제6장 전세권")),
                anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("retrieve — lawIds가 주어지면 *ByLaws 쿼리로 분기")
    void retrieve_withLawIds_usesByLawsVariant() {
        when(queryEmbeddingService.embedQuery(anyString()))
                .thenReturn(new float[1024]);
        when(repository.search3WayByLaws(
                anyString(), anyString(), anyString(), nullable(String[].class),
                anyCollection(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        service.retrieve("임대차", List.of("임대차"), null, List.of("law-civil"), 5);

        verify(repository, never()).search3Way(
                anyString(), anyString(), anyString(), nullable(String[].class),
                anyDouble(), anyDouble(), anyDouble(), anyInt());
        verify(repository).search3WayByLaws(
                anyString(), anyString(), anyString(), nullable(String[].class),
                argThat(c -> c.contains("law-civil")),
                anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("retrieve — vectorQuery/bm25Keywords 모두 비면 즉시 빈 리스트")
    void retrieve_emptyInputs_returnsEmpty() {
        List<LegalChunk> result = service.retrieve("", List.of(), null, null, 5);
        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
        verifyNoInteractions(queryEmbeddingService);
    }

    @Test
    @DisplayName("하위호환 4-인자 retrieve는 categoryIds=null로 5-인자에 위임한다 (LegalRetrievalService 인터페이스 기본 메서드)")
    void legacyRetrieve_delegatesWithNullCategoryIds() {
        when(queryEmbeddingService.embedQuery(anyString()))
                .thenReturn(new float[1024]);
        when(repository.search3Way(
                anyString(), anyString(), anyString(), nullable(String[].class),
                anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        LegalRetrievalService as = service;
        as.retrieve("임대차", List.of("임대차"), null, 3);

        verify(repository).search3Way(
                anyString(), anyString(), anyString(),
                isNull(),  // categoryIds
                anyDouble(), anyDouble(), anyDouble(), eq(3));
    }

    // ------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------

    private static LegalChunkRow row(String lawName, String articleNo, String articleTitle,
                                     String content, String effectiveDate, String sourceUrl,
                                     double score) {
        return new LegalChunkRow() {
            @Override public String getLawName() { return lawName; }
            @Override public String getArticleNo() { return articleNo; }
            @Override public String getArticleTitle() { return articleTitle; }
            @Override public String getContent() { return content; }
            @Override public String getEffectiveDate() { return effectiveDate; }
            @Override public String getSourceUrl() { return sourceUrl; }
            @Override public Double getScore() { return score; }
        };
    }
}
