package org.example.shield.ai.application;

import org.example.shield.ai.dto.LegalChunk;

import java.util.List;

/**
 * Layer 2: 법률 검색 서비스 인터페이스.
 * Phase A에서 RAG 스토리지를 PostgreSQL로 이관하였고,
 * PG 기반 하이브리드 검색(복수 후보 결합) 구현체가 이 인터페이스를 대체한다.
 * 현재 기본은 Stub 구현이고, 실제 구현체는 Phase A-5에서 등록된다.
 */
public interface LegalRetrievalService {

    /**
     * 하이브리드 검색 (벡터 + BM25).
     *
     * @param vectorQuery   벡터 검색용 자연어 쿼리
     * @param bm25Keywords  BM25 키워드 검색용 핵심 키워드
     * @param lawIds        검색 범위를 한정하는 법령 ID 목록
     * @param topK          반환할 최대 청크 수
     * @return 관련 법률 조문 청크 목록
     */
    List<LegalChunk> retrieve(
            String vectorQuery,
            List<String> bm25Keywords,
            List<String> lawIds,
            int topK
    );
}
