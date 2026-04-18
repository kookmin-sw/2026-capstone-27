package org.example.shield.ai.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.dto.LegalChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LegalRetrievalService Stub 구현.
 * Phase A에서 MongoDB를 제거하였고, PostgreSQL 기반 retrieval 구현체가 등록되기 전까지
 * 기본으로 활성화되어 빈 리스트를 반환한다.
 *
 * rag.retrieval.stub=true (기본값) 일 때 활성화.
 * PG 기반 실제 구현체가 들어오면 rag.retrieval.stub=false 로 전환.
 */
@Component
@ConditionalOnProperty(name = "rag.retrieval.stub", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StubLegalRetrievalService implements LegalRetrievalService {

    @Override
    public List<LegalChunk> retrieve(String vectorQuery, List<String> bm25Keywords,
                                     List<String> lawIds, int topK) {
        log.warn("RAG retrieval stub 활성화 — 실제 검색 스킵. vectorQuery='{}', lawIds={}, topK={}",
                vectorQuery, lawIds, topK);
        return List.of();
    }
}
