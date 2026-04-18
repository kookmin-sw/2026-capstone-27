package org.example.shield.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
