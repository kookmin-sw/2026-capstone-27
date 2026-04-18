package org.example.shield.ai.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.dto.IntentClassificationResult;
import org.example.shield.ai.dto.LegalChunk;
import org.example.shield.consultation.domain.Message;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 파이프라인 오케스트레이터.
 * Layer 1(의도 분류) → Layer 2(법률 검색) → Layer 3(컨텍스트 빌드) 를 조율.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagPipelineService {

    private final IntentClassificationService intentClassificationService;
    private final CategoryLawMappingService categoryLawMappingService;
    private final LegalRetrievalService legalRetrievalService;
    private final RagContextBuilder ragContextBuilder;

    /**
     * RAG 파이프라인 실행. 실패 시 빈 문자열 반환 (폴백).
     *
     * @param chatHistory    대화 내역
     * @param primaryField   상담 분야
     * @param consultationId 로깅용 상담 ID
     * @return RAG 컨텍스트 문자열 (실패 시 빈 문자열)
     */
    public String execute(List<Message> chatHistory, String primaryField, Object consultationId) {
        try {
            // Layer 1: 의도 분류
            IntentClassificationResult classification =
                    intentClassificationService.classify(chatHistory, primaryField);

            // Layer 2: 법률 검색
            List<String> lawIds = categoryLawMappingService.resolveLawIds(
                    classification.matchedNodeIds());
            String vectorQuery = classification.retrievalQueries().isEmpty()
                    ? primaryField + " 관련 법률"
                    : classification.retrievalQueries().get(0);
            List<LegalChunk> chunks = legalRetrievalService.retrieve(
                    vectorQuery,
                    classification.keywords().core(),
                    lawIds, 3);

            // Layer 3: 컨텍스트 빌드
            String ragContext = ragContextBuilder.build(chunks, classification.intentSummary());
            if (!ragContext.isEmpty()) {
                log.info("RAG 컨텍스트 생성 완료: consultationId={}, chunks={}", consultationId, chunks.size());
            }
            return ragContext;

        } catch (Exception e) {
            log.warn("RAG 파이프라인 실패, 폴백 (RAG 없이 진행): consultationId={}, error={}",
                    consultationId, e.getMessage());
            return "";
        }
    }
}
