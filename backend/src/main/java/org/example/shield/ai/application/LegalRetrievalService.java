package org.example.shield.ai.application;

import org.example.shield.ai.dto.LegalChunk;
import org.example.shield.ai.dto.MixedRetrievalResult;

import java.util.ArrayList;
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

    /**
     * 법령 + 판례 병합 하이브리드 검색 (C-5, Issue #42).
     *
     * <p>각 코퍼스에서 top-K 를 뽑아 {@code score DESC} 기준으로 merge 한 뒤 상위 {@code topK} 만
     * 반환한다. Phase C-4 벤치마크에서 MRR +0.246 / nDCG@5 +0.250 가 검증된 경로다.</p>
     *
     * <p>기본 구현은 기존 {@link #retrieve} 만 호출해 법령 결과만 담아 반환한다 — Stub 구현체
     * 처럼 판례 검색을 지원하지 않는 구현체에서도 안전하게 동작하도록 하기 위함. 실제 DB
     * 기반 구현체({@code PgLegalRetrievalService}) 가 override 해서 판례까지 포함한다.</p>
     *
     * @param vectorQuery   벡터/BM25 검색용 자연어 쿼리
     * @param bm25Keywords  BM25 핵심 키워드
     * @param categoryIds   카테고리 필터 (법령·판례 공통)
     * @param lawIds        법령 ID 필터 — 판례에는 적용되지 않음
     * @param topK          상위 반환 개수 (병합 결과 기준)
     * @return 법령·판례 별도 리스트와 병합 리스트를 모두 가진 결과 객체
     */
    default MixedRetrievalResult retrieveMixed(
            String vectorQuery,
            List<String> bm25Keywords,
            List<String> categoryIds,
            List<String> lawIds,
            int topK
    ) {
        List<LegalChunk> laws = retrieve(vectorQuery, bm25Keywords, categoryIds, lawIds, topK);
        List<org.example.shield.ai.dto.RetrievedDocument> merged = new ArrayList<>(laws);
        return new MixedRetrievalResult(laws, List.of(), List.copyOf(merged));
    }
}
