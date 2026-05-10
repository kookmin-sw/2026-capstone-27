package org.example.shield.lawyer.infrastructure;

import org.example.shield.lawyer.domain.LawyerEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * 변호사 임베딩 리포지토리 (Issue #50).
 *
 * <p>pgvector 코사인 거리 연산자 {@code <=>} 를 이용해
 * 쿼리 벡터에 가까운 VERIFIED 변호사 목록을 유사도 내림차순으로 반환한다.</p>
 *
 * <p>쿼리 벡터는 {@code PgLegalRetrievalService.floatArrayToPgVector} 와 동일한
 * {@code "[0.123456,...]"} 문자열 포맷으로 전달하며, SQL 에서 {@code CAST(:queryVec AS vector)} 로 변환한다.</p>
 */
public interface LawyerEmbeddingRepository extends JpaRepository<LawyerEmbedding, UUID> {

    /**
     * 코사인 유사도 기반 상위 변호사 조회 (VERIFIED 만).
     *
     * <p>{@code similarity = 1 - cosine_distance}, 즉 1 에 가까울수록 유사.
     * HNSW 인덱스 (vector_cosine_ops) 를 타기 위해 ORDER BY 는 {@code <=>} 연산자 그대로 사용.</p>
     */
    @Query(value = """
            SELECT l.id AS lawyerId,
                   l.user_id AS userId,
                   l.domains AS domains,
                   l.sub_domains AS subDomains,
                   l.tags AS tags,
                   l.bio AS bio,
                   l.experience_years AS experienceYears,
                   l.region AS region,
                   1 - (e.embedding <=> CAST(:queryVec AS vector)) AS similarity
            FROM lawyer_embeddings e
            JOIN lawyers l ON l.id = e.lawyer_id
            WHERE CAST(l.verification_status AS TEXT) = 'VERIFIED'
            ORDER BY e.embedding <=> CAST(:queryVec AS vector)
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<LawyerMatchProjection> findTopBySimilarity(
            @Param("queryVec") String queryVec,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * VERIFIED 변호사 중 임베딩이 존재하는 총 개수 (페이지네이션 total).
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM lawyer_embeddings e
            JOIN lawyers l ON l.id = e.lawyer_id
            WHERE CAST(l.verification_status AS TEXT) = 'VERIFIED'
            """, nativeQuery = true)
    long countVerifiedWithEmbedding();
}
