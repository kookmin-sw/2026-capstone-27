package org.example.shield.ai.dto;

/**
 * RAG 검색 결과의 공통 인터페이스. Phase C-5 (Issue #42) 도입.
 *
 * <p>법령 조문({@link LegalChunk}) 과 판례({@link Precedent}) 를 함께 다루기 위한 sealed 타입.
 * {@link LegalChunk} 는 기존 코드 호환을 위해 유지하면서 이 인터페이스를 추가로 구현한다.</p>
 *
 * <p>{@code kind()} 는 프롬프트 직렬화와 로깅에서 두 타입을 분기하는 용도로 사용된다.
 * <ul>
 *   <li>{@code "law"} — 법령 조문 ({@link LegalChunk})</li>
 *   <li>{@code "case"} — 판례 ({@link Precedent})</li>
 * </ul>
 * </p>
 *
 * <p>Java sealed 타입 문법으로 컴파일러가 두 구현체 외 확장을 차단한다.</p>
 */
public sealed interface RetrievedDocument
        permits LegalChunk, Precedent {

    /** 문서 유형 식별자. 프롬프트 섹션 분기·로깅에 사용. */
    String kind();

    /** 하이브리드 검색 점수 (법령·판례 모두 동일 가중치 스키마로 계산되므로 비교 가능). */
    double score();
}
