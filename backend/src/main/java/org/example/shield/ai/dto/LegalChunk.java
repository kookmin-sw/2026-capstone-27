package org.example.shield.ai.dto;

/**
 * 법률 조문 청크 데이터.
 * Layer 2 벡터 검색 결과로 반환되는 개별 법률 조문 단위.
 *
 * <p>Phase C-5 (Issue #42) 부터 {@link RetrievedDocument} sealed interface 를 구현하여
 * 판례({@link Precedent}) 와 함께 RAG 컨텍스트 빌더에서 다루어진다.
 * 기존 필드 시그니처는 그대로 유지되어 하위 호환성에 영향이 없다.</p>
 */
public record LegalChunk(
        String lawName,
        String articleNo,
        String articleTitle,
        String content,
        String effectiveDate,
        String sourceUrl,
        double score
) implements RetrievedDocument {

    @Override
    public String kind() {
        return "law";
    }
}
