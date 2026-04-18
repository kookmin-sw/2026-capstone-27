package org.example.shield.ai.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.LegalRetrievalService;
import org.example.shield.ai.domain.LegalChunkJpaRepository;
import org.example.shield.ai.domain.LegalChunkJpaRepository.LegalChunkRow;
import org.example.shield.ai.dto.LegalChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PostgreSQL 기반 Layer 2 법률 검색 구현.
 *
 * <p>현재 단계에서는 {@code content_tsv}(tsvector GENERATED) 전문 검색과
 * {@code pg_trgm} 유사도를 결합한 하이브리드 랭킹을 수행한다.
 * 벡터 임베딩 컬럼은 후속 단계에서 도입되며, 그때 이 클래스의 쿼리가 확장된다.</p>
 *
 * <p>활성화 조건: {@code rag.retrieval.stub=false} (application.yml 기본값).
 * Stub 구현과 상호 배타적으로 동작한다.</p>
 */
@Service
@ConditionalOnProperty(name = "rag.retrieval.stub", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class PgLegalRetrievalService implements LegalRetrievalService {

    private final LegalChunkJpaRepository legalChunkJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LegalChunk> retrieve(String vectorQuery,
                                     List<String> bm25Keywords,
                                     List<String> lawIds,
                                     int topK) {

        String safeVectorQuery = (vectorQuery == null || vectorQuery.isBlank())
                ? ""
                : vectorQuery.trim();
        String keywordQuery = buildKeywordTsQuery(bm25Keywords, safeVectorQuery);
        int safeTopK = Math.max(1, topK);

        if (safeVectorQuery.isEmpty() && keywordQuery.isEmpty()) {
            log.debug("RAG 검색 스킵 — vectorQuery/bm25Keywords 모두 비어 있음");
            return List.of();
        }

        // tsquery는 빈 문자열을 받으면 parse error가 발생하므로 sentinel로 대체
        // (plainto_tsquery/to_tsquery 모두 토큰이 하나라도 있어야 안전)
        String vq = safeVectorQuery.isEmpty() ? "__none__" : safeVectorQuery;
        String kq = keywordQuery.isEmpty()    ? "__none__" : keywordQuery;

        List<LegalChunkRow> rows;
        if (lawIds == null || lawIds.isEmpty()) {
            rows = legalChunkJpaRepository.searchHybrid(vq, kq, safeTopK);
        } else {
            rows = legalChunkJpaRepository.searchHybridByLaws(vq, kq, lawIds, safeTopK);
        }

        log.debug("RAG PG 검색 완료 — vq='{}', kq='{}', lawIds={}, hits={}",
                vq, kq, lawIds, rows.size());

        return rows.stream()
                .map(r -> new LegalChunk(
                        nz(r.getLawName()),
                        nz(r.getArticleNo()),
                        nz(r.getArticleTitle()),
                        nz(r.getContent()),
                        nz(r.getEffectiveDate()),
                        nz(r.getSourceUrl()),
                        r.getScore() == null ? 0.0 : r.getScore()))
                .toList();
    }

    /**
     * bm25Keywords를 {@code to_tsquery('simple', ...)}가 받을 수 있는 OR 조합 문자열로 변환한다.
     * - 각 키워드는 공백/특수문자 정리 후 '|'로 결합
     * - 모든 키워드가 제거되면 빈 문자열 반환
     * - 키워드가 없고 vectorQuery만 있을 경우에는 vectorQuery의 첫 토큰을 fallback으로 사용
     */
    private String buildKeywordTsQuery(List<String> bm25Keywords, String vectorQuery) {
        List<String> sanitized = (bm25Keywords == null ? List.<String>of() : bm25Keywords).stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeToken)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (!sanitized.isEmpty()) {
            return sanitized.stream().collect(Collectors.joining(" | "));
        }

        // fallback: vectorQuery의 첫 토큰 하나만 키워드로 사용
        if (!vectorQuery.isEmpty()) {
            String first = sanitizeToken(vectorQuery.split("\\s+")[0]);
            if (!first.isEmpty()) {
                return first;
            }
        }
        return "";
    }

    /**
     * tsquery 연산자/구두점을 제거한다. 'simple' regconfig 대응.
     * 허용: 한글/영문/숫자/공백 → 공백은 제거(단일 토큰화).
     */
    private String sanitizeToken(String raw) {
        if (raw == null) return "";
        // tsquery 메타 문자(&|!():*<>) 및 따옴표/백슬래시 제거
        String cleaned = raw.replaceAll("[\\s&|!():*<>\"'\\\\]", "");
        return cleaned;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
