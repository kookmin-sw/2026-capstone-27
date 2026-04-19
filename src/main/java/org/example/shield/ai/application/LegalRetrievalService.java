package org.example.shield.ai.application;

import org.example.shield.ai.dto.LegalChunk;

import java.util.List;

/**
 * Layer 2: 법률 검색 서비스 인터페이스.
 *
 * <p>Phase A에서 RAG 스토리지를 PostgreSQL로 이관하였고, B-4에서
 * pgvector 기반 벡터 검색 경로가 추가되어 3-way 하이브리드(벡터 + BM25 + 트라이그램)로
 * 확장되었다.</p>
 *
 * <p>구현체는 두 가지가 공존한다.
 * <ul>
 *   <li>{@code StubLegalRetrievalService} — {@code rag.retrieval.stub=true} (기본값)
 *   <li>{@code PgLegalRetrievalService}   — {@code rag.retrieval.stub=false}
 * </ul>
 * </p>
 */
public interface LegalRetrievalService {

    /**
     * 하이브리드 검색 (벡터 + BM25 + 트라이그램).
     *
     * <p>Phase A 호환 시그니처. 호출 시 {@code categoryIds=null}로
     * {@link #retrieve(String, List, List, List, int)}에 위임한다.</p>
     *
     * @param vectorQuery   벡터 검색용 자연어 쿼리
     * @param bm25Keywords  BM25 키워드 검색용 핵심 키워드
     * @param lawIds        검색 범위를 한정하는 법령 ID 목록 (null/empty 허용)
     * @param topK          반환할 최대 청크 수
     * @return 관련 법률 조문 청크 목록
     */
    default List<LegalChunk> retrieve(
            String vectorQuery,
            List<String> bm25Keywords,
            List<String> lawIds,
            int topK
    ) {
        return retrieve(vectorQuery, bm25Keywords, null, lawIds, topK);
    }

    /**
     * 3-way 하이브리드 검색 (B-4 확장).
     *
     * <p>검색 경로:</p>
     * <ul>
     *   <li>벡터: {@code embedding}(pgvector) cosine similarity
     *   <li>BM25: {@code content_tsv} tsvector ts_rank
     *   <li>트라이그램: {@code pg_trgm similarity} (오탈자/부분 일치 보조)
     * </ul>
     *
     * <p>{@code categoryIds}는 하드 필터가 아닌 soft-filter로 작동:
     * 지정된 경우 해당 카테고리에 속하는 조문을 우선 후보로 포함하되,
     * 매칭이 충분치 않으면 범위 밖 고점수 후보도 허용한다 (구현체 재량).</p>
     *
     * @param vectorQuery   벡터/BM25 검색용 자연어 쿼리
     * @param bm25Keywords  BM25 키워드 (core terms)
     * @param categoryIds   category_ids 필터. {@code book:...}, {@code chapter:...},
     *                      {@code group:...} 등. null/empty면 필터 미적용.
     * @param lawIds        법령 ID 필터. null/empty면 전체.
     * @param topK          반환할 최대 청크 수
     * @return 관련 법률 조문 청크 목록 (score 내림차순)
     */
    List<LegalChunk> retrieve(
            String vectorQuery,
            List<String> bm25Keywords,
            List<String> categoryIds,
            List<String> lawIds,
            int topK
    );
}
