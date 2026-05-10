package org.example.shield.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * legal_chunks 테이블에 대한 Spring Data JPA 리포지토리.
 *
 * <p>Layer 2 벡터/전문 검색은 A-5 단계에서 네이티브 쿼리로 추가된다.
 * 본 인터페이스는 기본 CRUD 및 식별 조회만 제공한다.</p>
 */
public interface LegalChunkJpaRepository extends JpaRepository<LegalChunkEntity, Long> {

    /**
     * (law_id, article_no, chunk_index) 조합으로 활성(폐지되지 않은) 청크 조회.
     * DB의 부분 유니크 제약과 일치한다.
     */
    @Query("""
            select lc
              from LegalChunkEntity lc
             where lc.lawId = :lawId
               and lc.articleNo = :articleNo
               and lc.chunkIndex = :chunkIndex
               and lc.abolitionDate is null
            """)
    Optional<LegalChunkEntity> findActiveByNaturalKey(@Param("lawId") String lawId,
                                                     @Param("articleNo") String articleNo,
                                                     @Param("chunkIndex") Short chunkIndex);

    /**
     * 특정 법령의 모든 활성 청크를 chunk_index 오름차순으로 조회.
     */
    @Query("""
            select lc
              from LegalChunkEntity lc
             where lc.lawId = :lawId
               and lc.abolitionDate is null
             order by lc.articleNo asc, lc.chunkIndex asc
            """)
    List<LegalChunkEntity> findActiveByLawId(@Param("lawId") String lawId);

    // ---------------------------------------------------------------------
    // Layer 2 하이브리드 검색 (B-4: 3-way — pgvector + BM25 + pg_trgm)
    //
    // 설계:
    //  - pgvector: 1 - (embedding <=> :queryVector::vector) → cosine similarity (0~1)
    //    * embedding IS NULL 행은 0으로 처리 (인제스트 미완료 조문 안전화)
    //  - BM25: to_tsquery('simple', :keywordQuery) ts_rank(...,1)
    //    * 길이 정규화 1 = "rank / (1 + log(doc length))"
    //  - 트라이그램: pg_trgm similarity(content, :vectorQuery) 보조
    //  - 점수 합산:
    //      score = vector_sim  * :vectorWeight
    //            + keyword_rank * :keywordWeight
    //            + trigram_sim  * :trigramWeight
    //
    // 필터 조합:
    //  - category_ids: legal_chunks.category_ids && :categoryIds (array overlap)
    //    * null/empty는 SQL 레벨에서 코알레스 → 미적용과 동등
    //  - law_ids:     :lawIds의 유무에 따라 두 개 쿼리로 분기 (빈 IN 회피)
    //
    // 후보 축소: 3가지 경로 중 어느 하나라도 매칭되는 행만 정렬 대상에 포함
    //
    // 투영(projection): LegalChunkRow → Service에서 LegalChunk record 변환
    // ---------------------------------------------------------------------

    /**
     * 3-way 하이브리드 검색 (법령ID 필터 없음).
     *
     * <p>{@code :queryVector}는 pgvector 리터럴 문자열 형식 {@code "[0.1,0.2,...]"}로 전달.
     * 서비스 레이어에서 {@code float[]} → 문자열 변환을 담당한다.</p>
     *
     * <p>{@code :categoryIds}는 {@code String[]} 배열. null/빈 배열이면 필터가 무시된다.
     * PostgreSQL 배열 겹침 연산자 {@code &&}는 한 원소라도 일치하면 true.</p>
     */
    @Query(value = """
            SELECT lc.law_name        AS lawName,
                   lc.article_no      AS articleNo,
                   lc.article_title   AS articleTitle,
                   lc.content         AS content,
                   to_char(lc.effective_date, 'YYYY-MM-DD') AS effectiveDate,
                   lc.source_url      AS sourceUrl,
                   ( COALESCE(CASE WHEN lc.embedding IS NULL THEN 0
                                   ELSE 1 - (lc.embedding <=> CAST(:queryVector AS vector))
                               END, 0) * :vectorWeight
                    + ts_rank(lc.content_tsv, to_tsquery('simple', :keywordQuery), 1) * :keywordWeight
                    + similarity(lc.content, :vectorQuery) * :trigramWeight) AS score
              FROM legal_chunks lc
             WHERE lc.abolition_date IS NULL
               AND ( COALESCE(CARDINALITY(CAST(:categoryIds AS text[])), 0) = 0
                     OR lc.category_ids && CAST(:categoryIds AS text[]) )
               AND ( lc.content_tsv @@ plainto_tsquery('simple', :vectorQuery)
                  OR lc.content_tsv @@ to_tsquery('simple', :keywordQuery)
                  OR lc.content %% :vectorQuery
                  OR lc.embedding IS NOT NULL )
             ORDER BY score DESC
             LIMIT :topK
            """, nativeQuery = true)
    List<LegalChunkRow> search3Way(@Param("queryVector") String queryVector,
                                   @Param("vectorQuery") String vectorQuery,
                                   @Param("keywordQuery") String keywordQuery,
                                   @Param("categoryIds") String[] categoryIds,
                                   @Param("vectorWeight") double vectorWeight,
                                   @Param("keywordWeight") double keywordWeight,
                                   @Param("trigramWeight") double trigramWeight,
                                   @Param("topK") int topK);

    /**
     * 3-way 하이브리드 검색 (법령ID 필터 포함).
     */
    @Query(value = """
            SELECT lc.law_name        AS lawName,
                   lc.article_no      AS articleNo,
                   lc.article_title   AS articleTitle,
                   lc.content         AS content,
                   to_char(lc.effective_date, 'YYYY-MM-DD') AS effectiveDate,
                   lc.source_url      AS sourceUrl,
                   ( COALESCE(CASE WHEN lc.embedding IS NULL THEN 0
                                   ELSE 1 - (lc.embedding <=> CAST(:queryVector AS vector))
                               END, 0) * :vectorWeight
                    + ts_rank(lc.content_tsv, to_tsquery('simple', :keywordQuery), 1) * :keywordWeight
                    + similarity(lc.content, :vectorQuery) * :trigramWeight) AS score
              FROM legal_chunks lc
             WHERE lc.abolition_date IS NULL
               AND lc.law_id IN (:lawIds)
               AND ( COALESCE(CARDINALITY(CAST(:categoryIds AS text[])), 0) = 0
                     OR lc.category_ids && CAST(:categoryIds AS text[]) )
               AND ( lc.content_tsv @@ plainto_tsquery('simple', :vectorQuery)
                  OR lc.content_tsv @@ to_tsquery('simple', :keywordQuery)
                  OR lc.content %% :vectorQuery
                  OR lc.embedding IS NOT NULL )
             ORDER BY score DESC
             LIMIT :topK
            """, nativeQuery = true)
    List<LegalChunkRow> search3WayByLaws(@Param("queryVector") String queryVector,
                                         @Param("vectorQuery") String vectorQuery,
                                         @Param("keywordQuery") String keywordQuery,
                                         @Param("categoryIds") String[] categoryIds,
                                         @Param("lawIds") Collection<String> lawIds,
                                         @Param("vectorWeight") double vectorWeight,
                                         @Param("keywordWeight") double keywordWeight,
                                         @Param("trigramWeight") double trigramWeight,
                                         @Param("topK") int topK);

    // ---------------------------------------------------------------------
    // [Legacy] B-1 이전의 2-way 하이브리드 (BM25 + trigram) — 하위 호환용.
    // 벡터 경로가 없어도 동작해야 하는 테스트/디버깅 루트.
    // ---------------------------------------------------------------------

    /** 법령ID 필터 없음 버전 */
    @Query(value = """
            SELECT lc.law_name        AS lawName,
                   lc.article_no      AS articleNo,
                   lc.article_title   AS articleTitle,
                   lc.content         AS content,
                   to_char(lc.effective_date, 'YYYY-MM-DD') AS effectiveDate,
                   lc.source_url      AS sourceUrl,
                   (ts_rank(lc.content_tsv, plainto_tsquery('simple', :vectorQuery), 1) * :vectorWeight
                    + ts_rank(lc.content_tsv, to_tsquery('simple', :keywordQuery), 1) * :keywordWeight
                    + similarity(lc.content, :vectorQuery) * :trigramWeight) AS score
              FROM legal_chunks lc
             WHERE lc.abolition_date IS NULL
               AND ( lc.content_tsv @@ plainto_tsquery('simple', :vectorQuery)
                  OR lc.content_tsv @@ to_tsquery('simple', :keywordQuery)
                  OR lc.content %% :vectorQuery )
             ORDER BY score DESC
             LIMIT :topK
            """, nativeQuery = true)
    List<LegalChunkRow> searchHybrid(@Param("vectorQuery") String vectorQuery,
                                     @Param("keywordQuery") String keywordQuery,
                                     @Param("vectorWeight") double vectorWeight,
                                     @Param("keywordWeight") double keywordWeight,
                                     @Param("trigramWeight") double trigramWeight,
                                     @Param("topK") int topK);

    /** 법령ID 필터 포함 버전 */
    @Query(value = """
            SELECT lc.law_name        AS lawName,
                   lc.article_no      AS articleNo,
                   lc.article_title   AS articleTitle,
                   lc.content         AS content,
                   to_char(lc.effective_date, 'YYYY-MM-DD') AS effectiveDate,
                   lc.source_url      AS sourceUrl,
                   (ts_rank(lc.content_tsv, plainto_tsquery('simple', :vectorQuery), 1) * :vectorWeight
                    + ts_rank(lc.content_tsv, to_tsquery('simple', :keywordQuery), 1) * :keywordWeight
                    + similarity(lc.content, :vectorQuery) * :trigramWeight) AS score
              FROM legal_chunks lc
             WHERE lc.abolition_date IS NULL
               AND lc.law_id IN (:lawIds)
               AND ( lc.content_tsv @@ plainto_tsquery('simple', :vectorQuery)
                  OR lc.content_tsv @@ to_tsquery('simple', :keywordQuery)
                  OR lc.content %% :vectorQuery )
             ORDER BY score DESC
             LIMIT :topK
            """, nativeQuery = true)
    List<LegalChunkRow> searchHybridByLaws(@Param("vectorQuery") String vectorQuery,
                                           @Param("keywordQuery") String keywordQuery,
                                           @Param("lawIds") Collection<String> lawIds,
                                           @Param("vectorWeight") double vectorWeight,
                                           @Param("keywordWeight") double keywordWeight,
                                           @Param("trigramWeight") double trigramWeight,
                                           @Param("topK") int topK);

    /**
     * Spring Data JPA 네이티브 쿼리용 projection 인터페이스.
     * 서비스 레이어에서 LegalChunk record 로 변환해 반환한다.
     */
    interface LegalChunkRow {
        String getLawName();
        String getArticleNo();
        String getArticleTitle();
        String getContent();
        String getEffectiveDate();
        String getSourceUrl();
        Double getScore();
    }
}
