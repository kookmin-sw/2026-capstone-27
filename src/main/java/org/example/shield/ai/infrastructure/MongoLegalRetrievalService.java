package org.example.shield.ai.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.domain.LegalChunkDocument;
import org.example.shield.ai.dto.LegalChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LegalRetrievalService 실제 구현체.
 * MongoDB $text 인덱스를 활용한 BM25 텍스트 검색 수행.
 * Atlas Search($search)가 사용 불가능한 환경에서는 표준 $text 쿼리로 폴백.
 *
 * 활성화 조건: rag.retrieval.stub=false
 */
@Component
@ConditionalOnProperty(name = "rag.retrieval.stub", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class MongoLegalRetrievalService implements LegalRetrievalService {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<LegalChunk> retrieve(String vectorQuery, List<String> bm25Keywords,
                                     List<String> lawIds, int topK) {
        String searchText = buildSearchText(bm25Keywords);
        if (searchText.isBlank()) {
            log.debug("검색 키워드가 비어있어 빈 결과 반환");
            return List.of();
        }

        try {
            return executeTextSearch(searchText, lawIds, topK);
        } catch (Exception e) {
            log.warn("MongoDB 텍스트 검색 실패 (텍스트 인덱스 미존재 가능): {}", e.getMessage());
            return List.of();
        }
    }

    private List<LegalChunk> executeTextSearch(String searchText, List<String> lawIds, int topK) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(searchText);

        Query query = TextQuery.queryText(textCriteria)
                .sortByScore();

        // law_id 필터
        if (lawIds != null && !lawIds.isEmpty()) {
            query.addCriteria(Criteria.where("law_id").in(lawIds));
        }

        // 폐지 법률 제외
        query.addCriteria(Criteria.where("abolition_date").is(null));

        query.limit(topK);

        List<LegalChunkDocument> documents = mongoTemplate.find(query, LegalChunkDocument.class);

        log.debug("MongoDB 텍스트 검색 결과: {}건 (검색어='{}', lawIds={})",
                documents.size(), searchText, lawIds);

        return documents.stream()
                .map(this::toLegalChunk)
                .toList();
    }

    private String buildSearchText(List<String> bm25Keywords) {
        if (bm25Keywords == null || bm25Keywords.isEmpty()) {
            return "";
        }
        return String.join(" ", bm25Keywords);
    }

    private LegalChunk toLegalChunk(LegalChunkDocument doc) {
        return new LegalChunk(
                doc.getLawName(),
                doc.getArticleNo(),
                doc.getArticleTitle(),
                doc.getContent(),
                doc.getEffectiveDate(),
                doc.getSourceUrl(),
                0.0  // textScore는 별도 projection 필요 — MVP에서는 0.0
        );
    }
}
