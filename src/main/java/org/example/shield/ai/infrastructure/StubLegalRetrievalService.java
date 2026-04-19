package org.example.shield.ai.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.dto.LegalChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LegalRetrievalService Stub 구현 — 개발/테스트 전용 opt-in (Phase B-7 이후).
 *
 * <p>Phase B-7 (2026-04-19) 이전에는 기본값이었지만, B-2 인제스트와 B-4 3-way
 * 하이브리드 검색 검증이 끝나면서 {@code rag.retrieval.stub}의 기본값이 {@code false}로
 * 전환되었다. 따라서 이 Stub은 {@code RAG_STUB=true} 환경변수를 명시적으로
 * 지정한 경우에만 등록된다.</p>
 *
 * <p>사용 시나리오:
 * <ul>
 *   <li>로컬 개발에서 Cohere API 키 없이 파이프라인을 돌려보고 싶을 때</li>
 *   <li>RAG 결과를 결정적으로 비운 상태로 두고 {@code MessageService}의
 *       fallback 경로를 검증할 때</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "rag.retrieval.stub", havingValue = "true", matchIfMissing = false)
@Slf4j
public class StubLegalRetrievalService implements LegalRetrievalService {

    @Override
    public List<LegalChunk> retrieve(String vectorQuery,
                                     List<String> bm25Keywords,
                                     List<String> categoryIds,
                                     List<String> lawIds,
                                     int topK) {
        log.warn("RAG retrieval stub 활성화 — 실제 검색 스킵. vectorQuery='{}', categoryIds={}, lawIds={}, topK={}",
                vectorQuery, categoryIds, lawIds, topK);
        return List.of();
    }
}
