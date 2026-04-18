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
    // Layer 2 하이브리드 검색 (tsvector BM25 + pg_trgm trigram 유사도)
    //
    // 설계:
    //  - content_tsv는 to_tsvector('simple', ...) GENERATED 컬럼이므로
    //    쿼리도 plainto_tsquery('simple', ...)로 대칭 구성한다.
    //  - :vectorQuery (자연어) → plainto_tsquery (AND)
    //  - :keywordQuery (키워드 ' | ' 조합) → to_tsquery (OR)
    //  - score = 자연어 ts_rank * 0.6 + 키워드 ts_rank * 0.3 + trigram similarity * 0.1
    //  - 법령ID 필터 유무에 따라 두 개의 쿼리로 분기 (빈 IN 회피)
    //  - 임베딩 컬럼(embedding vector)은 차후 단계에서 도입 예정
    //
    // 투영(constructor projection): LegalChunk record
    //   (lawName, articleNo, articleTitle, content, effectiveDate, sourceUrl, score)
    // ---------------------------------------------------------------------

    /** 법령ID 필터 없음 버전 */
    @Query(value = """
            SELECT lc.law_name        AS lawName,
                   lc.article_no      AS articleNo,
                   lc.article_title   AS articleTitle,
                   lc.content         AS content,
                   to_char(lc.effective_date, 'YYYY-MM-DD') AS effectiveDate,
                   lc.source_url      AS sourceUrl,
                   (ts_rank(lc.content_tsv, plainto_tsquery('simple', :vectorQuery)) * 0.6
                    + ts_rank(lc.content_tsv, to_tsquery('simple', :keywordQuery)) * 0.3
                    + similarity(lc.content, :vectorQuery) * 0.1) AS score
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
                                     @Param("topK") int topK);

    /** 법령ID 필터 포함 버전 */
    @Query(value = """
            SELECT lc.law_name        AS lawName,
                   lc.article_no      AS articleNo,
                   lc.article_title   AS articleTitle,
                   lc.content         AS content,
                   to_char(lc.effective_date, 'YYYY-MM-DD') AS effectiveDate,
                   lc.source_url      AS sourceUrl,
                   (ts_rank(lc.content_tsv, plainto_tsquery('simple', :vectorQuery)) * 0.6
                    + ts_rank(lc.content_tsv, to_tsquery('simple', :keywordQuery)) * 0.3
                    + similarity(lc.content, :vectorQuery) * 0.1) AS score
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
