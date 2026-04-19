package org.example.shield.ai.application;

import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.dto.IntentClassificationResult;
import org.example.shield.ai.dto.LegalChunk;
import org.example.shield.ai.dto.MixedRetrievalResult;
import org.example.shield.consultation.domain.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 파이프라인 오케스트레이터.
 * Layer 1(의도 분류) → Layer 2(법률·판례 검색) → Layer 3(컨텍스트 빌드) 를 조율.
 *
 * <p>{@code rag.retrieval.include-cases=true} 인 경우 C-5 경로로 법령 + 판례를 병합해 불러온다.
 * 기본값은 {@code false} 로 법령 전용 경로 유지 — Phase C-4 까지의 동작 호환성.</p>
 */
@Service
@Slf4j
public class RagPipelineService {

    private final IntentClassificationService intentClassificationService;
    private final CategoryLawMappingService categoryLawMappingService;
    private final LegalRetrievalService legalRetrievalService;
    private final RagContextBuilder ragContextBuilder;
    private final boolean includeCases;

    public RagPipelineService(IntentClassificationService intentClassificationService,
                              CategoryLawMappingService categoryLawMappingService,
                              LegalRetrievalService legalRetrievalService,
                              RagContextBuilder ragContextBuilder,
                              @Value("${rag.retrieval.include-cases:false}") boolean includeCases) {
        this.intentClassificationService = intentClassificationService;
        this.categoryLawMappingService = categoryLawMappingService;
        this.legalRetrievalService = legalRetrievalService;
        this.ragContextBuilder = ragContextBuilder;
        this.includeCases = includeCases;
        log.info("RagPipelineService 초기화 — include-cases={}", includeCases);
    }

    /**
     * RAG 파이프라인 실행. 실패 시 빈 문자열 반환 (폴백).
     *
     * @param chatHistory    대화 내역
     * @param domain         상담 대분류 (온톨로지 L1)
     * @param consultationId 로깅용 상담 ID
     * @return RAG 컨텍스트 문자열 (실패 시 빈 문자열)
     */
    public String execute(List<Message> chatHistory, String domain, Object consultationId) {
        try {
            // Layer 1: 의도 분류
            IntentClassificationResult classification =
                    intentClassificationService.classify(chatHistory, domain);

            // Layer 2: 법률 (+ 옵션: 판례) 검색
            List<String> lawIds = categoryLawMappingService.resolveLawIds(
                    classification.matchedNodeIds());
            String vectorQuery = classification.retrievalQueries().isEmpty()
                    ? domain + " 관련 법률"
                    : classification.retrievalQueries().get(0);

            String ragContext;
            int hits;
            if (includeCases) {
                MixedRetrievalResult mixed = legalRetrievalService.retrieveMixed(
                        vectorQuery,
                        classification.keywords().core(),
                        classification.matchedNodeIds(),
                        lawIds,
                        3);
                ragContext = ragContextBuilder.build(mixed, classification.intentSummary());
                hits = mixed.size();
                if (!ragContext.isEmpty()) {
                    log.info("RAG 컨텍스트 생성 완료 (mixed): consultationId={}, laws={}, cases={}, merged={}",
                            consultationId, mixed.laws().size(), mixed.cases().size(), hits);
                }
            } else {
                List<LegalChunk> chunks = legalRetrievalService.retrieve(
                        vectorQuery,
                        classification.keywords().core(),
                        lawIds, 3);
                ragContext = ragContextBuilder.build(chunks, classification.intentSummary());
                hits = chunks.size();
                if (!ragContext.isEmpty()) {
                    log.info("RAG 컨텍스트 생성 완료: consultationId={}, chunks={}", consultationId, hits);
                }
            }
            return ragContext;

        } catch (Exception e) {
            log.warn("RAG 파이프라인 실패, 폴백 (RAG 없이 진행): consultationId={}, error={}",
                    consultationId, e.getMessage());
            return "";
        }
    }
}
