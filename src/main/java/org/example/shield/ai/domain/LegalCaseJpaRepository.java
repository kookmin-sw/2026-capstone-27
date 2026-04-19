package org.example.shield.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * legal_cases 테이블 JPA 리포지토리.
 *
 * <p>본 리포지토리는 C-3 스키마 단계에서 기본 CRUD, 자연키 조회, 단순 필터만 제공한다.
 * 하이브리드 검색(pgvector + BM25 + pg_trgm)은 C-4 인제스트 이후 별도 검색 서비스에서
 * 네이티브 쿼리로 구현한다 — {@code LegalChunkJpaRepository#searchHybrid*}와 동일 패턴.</p>
 */
public interface LegalCaseJpaRepository extends JpaRepository<LegalCaseEntity, Long> {

    /**
     * (case_no, court, decision_date) 자연키로 판례 조회.
     * 자연키 유니크 제약은 Flyway V6에서 관리한다.
     */
    @Query("""
            select lc
              from LegalCaseEntity lc
             where lc.caseNo = :caseNo
               and lc.court = :court
               and lc.decisionDate = :decisionDate
            """)
    Optional<LegalCaseEntity> findByNaturalKey(@Param("caseNo") String caseNo,
                                               @Param("court") String court,
                                               @Param("decisionDate") LocalDate decisionDate);

    /**
     * 사건번호로 해당 사건의 모든 심급 판결 조회 (선고일 오름차순).
     */
    @Query("""
            select lc
              from LegalCaseEntity lc
             where lc.caseNo = :caseNo
             order by lc.decisionDate asc
            """)
    List<LegalCaseEntity> findAllByCaseNoOrderByDecisionDateAsc(@Param("caseNo") String caseNo);

    /**
     * 사건 유형 + 선고일 범위로 조회 (최근 판례 우선).
     */
    @Query("""
            select lc
              from LegalCaseEntity lc
             where lc.caseType = :caseType
               and lc.decisionDate between :from and :to
             order by lc.decisionDate desc
            """)
    List<LegalCaseEntity> findRecentByCaseType(@Param("caseType") String caseType,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /**
     * 임베딩이 아직 없는(C-4 백필 대상) 판례 개수.
     */
    @Query("""
            select count(lc)
              from LegalCaseEntity lc
             where lc.embedding is null
            """)
    long countMissingEmbedding();
}
