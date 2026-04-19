package org.example.shield.ai.infrastructure;

import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.application.QueryEmbeddingService;
import org.example.shield.ai.config.CohereApiConfig;
import org.example.shield.ai.domain.LegalChunkJpaRepository;
import org.example.shield.ai.domain.LegalChunkJpaRepository.LegalChunkRow;
import org.example.shield.ai.dto.LegalChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PostgreSQL 기반 Layer 2 법률 검색 구현 — 3-way 하이브리드 (B-4).
 *
 * <p>검색 경로:</p>
 * <ul>
 *   <li>벡터: Cohere embed-v4.0으로 쿼리 임베딩 생성 →
 *       {@code pgvector} cosine similarity ({@code <=>} 연산자)</li>
 *   <li>BM25: {@code content_tsv} (tsvector GENERATED) ts_rank</li>
 *   <li>트라이그램: {@code pg_trgm} similarity (오탈자/부분일치 보조)</li>
 * </ul>
 *
 * <p>선택 필터:</p>
 * <ul>
 *   <li>{@code categoryIds}: {@code legal_chunks.category_ids && ARRAY[...]} (배열 겹침)</li>
 *   <li>{@code lawIds}: {@code law_id IN (...)}</li>
 * </ul>
 *
 * <p>활성화 조건: {@code rag.retrieval.stub=false}.
 * 기본값은 Stub이며, 인제스트 완료 후 {@code RAG_STUB=false}로 전환.</p>
 *
 * <p>점수 가중치는 {@code rag.retrieval.weights.*}로 외부화되어 재빌드 없이 튜닝 가능하다.</p>
 *
 * <p>쿼리 임베딩 실패 시에는 영벡터로 대체되어 벡터 경로 점수가 0이 되고,
 * BM25 + 트라이그램 2-way로 자연스럽게 degrade된다. Cohere 장애 상황에서도
 * RAG 파이프라인 전체가 멈추지 않도록 하기 위함이다.</p>
 */
@Service
@ConditionalOnProperty(name = "rag.retrieval.stub", havingValue = "false", matchIfMissing = false)
@Slf4j
public class PgLegalRetrievalService implements LegalRetrievalService {

    /**
     * tsquery가 빈 문자열을 받으면 parse error가 발생하므로 사용하는 sentinel 토큰.
     * 실제 법률 문서에 존재할 가능성이 극히 낮은 형태로 구성.
     */
    private static final String EMPTY_QUERY_SENTINEL = "__shield_never_match__";

    private final LegalChunkJpaRepository legalChunkJpaRepository;
    private final QueryEmbeddingService queryEmbeddingService;
    private final RagMetrics ragMetrics;
    private final double vectorWeight;
    private final double keywordWeight;
    private final double trigramWeight;
    /** HNSW 인덱스 튜닝 파라미터. 1 이하이면 SET LOCAL 생략. */
    private final int hnswEfSearch;
    /** 1024차원 영벡터의 pgvector 리터럴 문자열 (벡터 경로 degrade 시 사용). */
    private final String zeroVectorLiteral;

    @PersistenceContext
    private EntityManager entityManager;

    public PgLegalRetrievalService(
            LegalChunkJpaRepository legalChunkJpaRepository,
            QueryEmbeddingService queryEmbeddingService,
            CohereApiConfig cohereConfig,
            RagMetrics ragMetrics,
            @Value("${rag.retrieval.weights.vector:0.5}") double vectorWeight,
            @Value("${rag.retrieval.weights.keyword:0.3}") double keywordWeight,
            @Value("${rag.retrieval.weights.trigram:0.2}") double trigramWeight,
            @Value("${rag.retrieval.hnsw.ef-search:40}") int hnswEfSearch) {
        this.legalChunkJpaRepository = legalChunkJpaRepository;
        this.queryEmbeddingService = queryEmbeddingService;
        this.ragMetrics = ragMetrics;
        this.vectorWeight = vectorWeight;
        this.keywordWeight = keywordWeight;
        this.trigramWeight = trigramWeight;
        this.hnswEfSearch = hnswEfSearch;
        this.zeroVectorLiteral = buildZeroVectorLiteral(cohereConfig.getEmbedDimension());
        log.info("PgLegalRetrievalService 활성화 (3-way) — weights(vector={}, keyword={}, trigram={}), embedDim={}, hnsw.ef_search={}",
                vectorWeight, keywordWeight, trigramWeight, cohereConfig.getEmbedDimension(), hnswEfSearch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalChunk> retrieve(String vectorQuery,
                                     List<String> bm25Keywords,
                                     List<String> categoryIds,
                                     List<String> lawIds,
                                     int topK) {
        Timer.Sample sample = ragMetrics.startRetrieve();
        try {
            List<LegalChunk> result = doRetrieve(vectorQuery, bm25Keywords, categoryIds, lawIds, topK);
            ragMetrics.stopRetrieveSuccess(sample, result.size());
            return result;
        } catch (RuntimeException e) {
            ragMetrics.stopRetrieveFailure(sample);
            throw e;
        }
    }

    private List<LegalChunk> doRetrieve(String vectorQuery,
                                        List<String> bm25Keywords,
                                        List<String> categoryIds,
                                        List<String> lawIds,
                                        int topK) {
        String safeVectorQuery = (vectorQuery == null || vectorQuery.isBlank())
                ? ""
                : vectorQuery.trim();
        String keywordQuery = buildKeywordTsQuery(bm25Keywords, safeVectorQuery);
        int safeTopK = Math.max(1, topK);

        if (safeVectorQuery.isEmpty() && keywordQuery.isEmpty()) {
            log.debug("RAG 검색 스킵 — vectorQuery/bm25Keywords 모두 비어 있음");
            return List.of();
        }

        // tsquery parse error 방지용 sentinel 대체
        String vq = safeVectorQuery.isEmpty() ? EMPTY_QUERY_SENTINEL : safeVectorQuery;
        String kq = keywordQuery.isEmpty()    ? EMPTY_QUERY_SENTINEL : keywordQuery;

        // 쿼리 임베딩 (실패 시 영벡터로 degrade → 벡터 경로 점수 0)
        String queryVectorLiteral = buildQueryVectorLiteral(safeVectorQuery);

        String[] categoryFilter = normalizeCategoryFilter(categoryIds);

        // HNSW ef_search 튜닝: 동일 트랜잭션 내 이후 실행되는 쿼리에만 적용.
        // @Transactional(readOnly=true)로 래핑된 본 메서드 범위 내에서 유효.
        applyHnswEfSearch();

        List<LegalChunkRow> rows;
        if (lawIds == null || lawIds.isEmpty()) {
            rows = legalChunkJpaRepository.search3Way(
                    queryVectorLiteral, vq, kq, categoryFilter,
                    vectorWeight, keywordWeight, trigramWeight, safeTopK);
        } else {
            rows = legalChunkJpaRepository.search3WayByLaws(
                    queryVectorLiteral, vq, kq, categoryFilter, lawIds,
                    vectorWeight, keywordWeight, trigramWeight, safeTopK);
        }

        log.debug("RAG 3-way 검색 완료 — vq='{}', kq='{}', categories={}, lawIds={}, hits={}",
                vq, kq, categoryFilter == null ? 0 : categoryFilter.length, lawIds, rows.size());

        return rows.stream()
                .map(r -> new LegalChunk(
                        nz(r.getLawName()),
                        nz(r.getArticleNo()),
                        nz(r.getArticleTitle()),
                        nz(r.getContent()),
                        nz(r.getEffectiveDate()),
                        nz(r.getSourceUrl()),
                        r.getScore() == null ? 0.0 : r.getScore()))
                .toList();
    }

    /**
     * 쿼리 문자열을 Cohere embed-v4.0으로 임베딩하여 pgvector 리터럴 문자열로 변환한다.
     * 빈 쿼리이거나 API 호출 실패 시 영벡터 리터럴을 반환하여 벡터 경로 점수를 0으로 만든다.
     */
    private String buildQueryVectorLiteral(String vectorQuery) {
        if (vectorQuery == null || vectorQuery.isEmpty()) {
            ragMetrics.recordVectorDegrade("empty_query");
            return zeroVectorLiteral;
        }
        try {
            float[] vec = queryEmbeddingService.embedQuery(vectorQuery);
            if (vec == null || vec.length == 0) {
                log.warn("쿼리 임베딩 응답이 비어 영벡터로 degrade: query='{}'", vectorQuery);
                ragMetrics.recordVectorDegrade("empty_response");
                return zeroVectorLiteral;
            }
            return floatArrayToPgVector(vec);
        } catch (Exception e) {
            log.warn("쿼리 임베딩 실패, 2-way(BM25+trigram)로 degrade: query='{}', error={}",
                    vectorQuery, e.getMessage());
            ragMetrics.recordVectorDegrade("cohere_error");
            return zeroVectorLiteral;
        }
    }

    /**
     * HNSW {@code ef_search} 세션 파라미터를 현재 트랜잭션에 적용한다.
     *
     * <p>pgvector 기본값 40. 1,193행 규모에서는 ef=40에서 topK=10 recall 100%가 확인되어
     * 기본값으로 40을 두고, 데이터가 커지면 {@code rag.retrieval.hnsw.ef-search}로
     * 80~200 범위에서 상향하는 것을 권장한다. 값은 적어도 topK 이상이어야 한다.</p>
     *
     * <p>{@code SET LOCAL}은 현재 트랜잭션에서만 유효하고 커넥션 풀 반납 시
     * 자동으로 해제되므로 다른 쿼리에 영향을 주지 않는다.</p>
     *
     * <p>HNSW 인덱스가 아직 생성되지 않았거나 파라미터가 비활성화된 환경에서도
     * 검색이 멈추지 않도록 예외는 삼키고 로그로만 남긴다.</p>
     */
    private void applyHnswEfSearch() {
        if (hnswEfSearch <= 1 || entityManager == null) {
            return;
        }
        try {
            entityManager.createNativeQuery("SET LOCAL hnsw.ef_search = " + hnswEfSearch).executeUpdate();
        } catch (Exception e) {
            log.debug("hnsw.ef_search 설정 실패 (무시): value={}, error={}", hnswEfSearch, e.getMessage());
        }
    }

    /** 테스트용 — EntityManager 주입 */
    void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    /**
     * categoryIds 리스트를 SQL 바인딩용 String[]로 변환.
     * null/빈 리스트면 null 반환 → 쿼리의 CARDINALITY 체크가 필터를 자동 무시.
     * 공백 요소는 제거.
     */
    private String[] normalizeCategoryFilter(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        String[] arr = categoryIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toArray(String[]::new);
        return arr.length == 0 ? null : arr;
    }

    /**
     * bm25Keywords를 {@code to_tsquery('simple', ...)}가 받을 수 있는 OR 조합 문자열로 변환한다.
     * - 각 키워드는 공백/특수문자 정리 후 '|'로 결합
     * - 모든 키워드가 제거되면 빈 문자열 반환
     * - 키워드가 없고 vectorQuery만 있을 경우에는 vectorQuery의 첫 토큰을 fallback으로 사용
     */
    private String buildKeywordTsQuery(List<String> bm25Keywords, String vectorQuery) {
        List<String> sanitized = (bm25Keywords == null ? List.<String>of() : bm25Keywords).stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeToken)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (!sanitized.isEmpty()) {
            return sanitized.stream().collect(Collectors.joining(" | "));
        }

        // fallback: vectorQuery의 첫 토큰 하나만 키워드로 사용
        if (!vectorQuery.isEmpty()) {
            String first = sanitizeToken(vectorQuery.split("\\s+")[0]);
            if (!first.isEmpty()) {
                return first;
            }
        }
        return "";
    }

    /**
     * tsquery 연산자/구두점을 제거한다. 'simple' regconfig 대응.
     * 허용: 한글/영문/숫자/공백 → 공백은 제거(단일 토큰화).
     */
    private String sanitizeToken(String raw) {
        if (raw == null) return "";
        // tsquery 메타 문자(&|!():*<>) 및 따옴표/백슬래시 제거
        String cleaned = raw.replaceAll("[\\s&|!():*<>\"'\\\\]", "");
        return cleaned;
    }

    /**
     * float[] 임베딩을 pgvector 리터럴 문자열 {@code "[0.1,0.2,...]"}로 변환.
     * PostgreSQL의 {@code CAST(:queryVector AS vector)}로 바인딩된다.
     */
    static String floatArrayToPgVector(float[] vec) {
        StringBuilder sb = new StringBuilder(vec.length * 10 + 2);
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            // 로케일에 의존하지 않는 점 구분 포맷
            sb.append(String.format(Locale.ROOT, "%.6f", vec[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    /** 영벡터(모든 차원이 0)의 pgvector 리터럴을 미리 생성. 초기화 시 1회만. */
    private static String buildZeroVectorLiteral(int dim) {
        StringBuilder sb = new StringBuilder(dim * 3 + 2);
        sb.append('[');
        for (int i = 0; i < dim; i++) {
            if (i > 0) sb.append(',');
            sb.append('0');
        }
        sb.append(']');
        return sb.toString();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
