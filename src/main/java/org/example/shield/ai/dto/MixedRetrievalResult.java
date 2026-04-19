package org.example.shield.ai.dto;

import java.util.List;

/**
 * 법령 + 판례 병합 검색 결과. Phase C-5 (Issue #42).
 *
 * <p>법령 검색과 판례 검색을 각각 top-K 뽑은 뒤 {@code score DESC} 기준으로 병합하여
 * 상위 {@code limit} 개를 {@link #merged()} 에 담는다. 원본 리스트({@link #laws()} /
 * {@link #cases()}) 도 함께 제공하여 디버깅·로깅·섹션별 프롬프트 구성에 활용 가능.</p>
 *
 * <p>{@code scripts/eval_rag.py} 의 UNION ALL 경로와 동일한 방식이지만, JDBC 매핑 단순화를
 * 위해 두 번의 쿼리를 날리고 메모리에서 병합한다. 두 쿼리 모두 인덱스를 잘 타므로 (HNSW +
 * GIN) 오버헤드는 무시할 만큼 작다.</p>
 *
 * @param laws   법령 조문 검색 결과 (score 내림차순)
 * @param cases  판례 검색 결과 (score 내림차순)
 * @param merged 두 리스트를 score 기준으로 합친 뒤 상위 {@code limit} 만 자른 결과
 */
public record MixedRetrievalResult(
        List<LegalChunk> laws,
        List<Precedent> cases,
        List<RetrievedDocument> merged
) {

    public static MixedRetrievalResult empty() {
        return new MixedRetrievalResult(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return merged == null || merged.isEmpty();
    }

    public int size() {
        return merged == null ? 0 : merged.size();
    }
}
