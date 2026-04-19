package org.example.shield.brief.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.QueryEmbeddingService;
import org.example.shield.brief.controller.dto.MatchingResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.exception.BriefNotFoundException;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.application.LawyerEmbeddingTextBuilder;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.infrastructure.LawyerEmbeddingRepository;
import org.example.shield.lawyer.infrastructure.LawyerMatchProjection;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 변호사 매칭 서비스 (Issue #50 리팩터).
 *
 * <p>Brief 의 {@code legalField}, {@code keywords}, {@code content} 를
 * {@link LawyerEmbeddingTextBuilder} 로 문서 측과 동일한 템플릿에 조립해 쿼리 벡터를 생성하고,
 * {@link LawyerEmbeddingRepository#findTopBySimilarity} 로 코사인 유사도 상위 변호사를 페이지네이션 반환한다.</p>
 *
 * <p>쿼리 임베딩 호출 실패 시 기존 키워드 매칭 fallback 으로 degrade.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerMatchingService {

    private final BriefReader briefReader;
    private final ConsultationReader consultationReader;
    private final LawyerReader lawyerReader;
    private final UserReader userReader;
    private final LawyerEmbeddingRepository lawyerEmbeddingRepository;
    private final LawyerEmbeddingTextBuilder embeddingTextBuilder;
    private final QueryEmbeddingService queryEmbeddingService;
    private final ObjectMapper objectMapper;

    public PageResponse<MatchingResponse> findMatching(UUID briefId, UUID userId, Pageable pageable) {
        Brief brief = briefReader.findById(briefId);
        if (!brief.getUserId().equals(userId)) {
            throw new BriefNotFoundException(briefId);
        }

        // 상담의 대/중/소분류를 우선 사용 (user > ai 폴백).
        Consultation consultation = consultationReader.findById(brief.getConsultationId());
        List<String> domains = consultation.getEffectiveDomains();
        List<String> subDomains = consultation.getEffectiveSubDomains();
        List<String> tags = consultation.getEffectiveTags();

        // 상담에 대분류 없으면 Brief.legalField 로 폴백.
        if (domains.isEmpty() && brief.getLegalField() != null) {
            domains = List.of(brief.getLegalField());
        }

        // matchedKeywords 집계용: 소분류 + Brief.keywords 합집합.
        List<String> briefKeywords = brief.getKeywords() != null ? brief.getKeywords() : Collections.emptyList();
        java.util.Set<String> keywordSet = new java.util.LinkedHashSet<>(tags);
        for (String kw : briefKeywords) {
            if (kw != null) keywordSet.add(kw);
        }
        List<String> matchKeywords = new java.util.ArrayList<>(keywordSet);

        // 1) 쿼리 텍스트 조립 → 임베딩
        String queryText = embeddingTextBuilder.build(
                domains,
                subDomains,
                tags,
                brief.getContent());

        float[] queryVec = tryEmbedQuery(queryText);
        if (queryVec != null && queryVec.length > 0) {
            Page<MatchingResponse> vectorPage = searchByVector(queryVec, matchKeywords, pageable);
            if (vectorPage != null) {
                return PageResponse.from(vectorPage);
            }
        }

        // 2) degrade: 기존 키워드 매칭 fallback
        log.warn("벡터 매칭 실패 → 키워드 fallback briefId={}", briefId);
        return fallbackKeywordMatching(matchKeywords, pageable);
    }

    private float[] tryEmbedQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }
        try {
            return queryEmbeddingService.embedQuery(queryText);
        } catch (Exception e) {
            log.warn("쿼리 임베딩 실패, fallback 전환 error={}", e.getMessage());
            return null;
        }
    }

    private Page<MatchingResponse> searchByVector(float[] queryVec,
                                                   List<String> briefKeywords,
                                                   Pageable pageable) {
        try {
            String vecLiteral = floatArrayToPgVector(queryVec);
            int limit = pageable.getPageSize();
            int offset = (int) pageable.getOffset();

            List<LawyerMatchProjection> rows = lawyerEmbeddingRepository.findTopBySimilarity(
                    vecLiteral, limit, offset);
            long total = lawyerEmbeddingRepository.countVerifiedWithEmbedding();

            List<UUID> userIds = rows.stream().map(LawyerMatchProjection::getUserId).toList();
            Map<UUID, User> userMap = userReader.findAllByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            List<MatchingResponse> responses = rows.stream()
                    .map(row -> toResponse(row, userMap, briefKeywords))
                    .toList();

            return new PageImpl<>(responses, pageable, total);
        } catch (Exception e) {
            log.warn("벡터 검색 실패 error={}", e.getMessage());
            return null;
        }
    }

    private MatchingResponse toResponse(LawyerMatchProjection row,
                                        Map<UUID, User> userMap,
                                        List<String> briefKeywords) {
        User user = userMap.get(row.getUserId());
        List<String> domains = parseJsonArray(row.getDomains());
        List<String> subDomains = parseJsonArray(row.getSubDomains());
        List<String> tags = parseJsonArray(row.getTags());
        List<String> matched = findMatchedKeywords(briefKeywords, tags);

        double score = row.getSimilarity() != null ? row.getSimilarity() : 0.0;

        return new MatchingResponse(
                row.getUserId(),
                user != null ? user.getName() : "알 수 없음",
                user != null ? user.getProfileImageUrl() : null,
                domains,
                subDomains,
                row.getExperienceYears(),
                tags,
                matched,
                row.getBio(),
                row.getRegion(),
                score
        );
    }

    private PageResponse<MatchingResponse> fallbackKeywordMatching(List<String> briefKeywords,
                                                                   Pageable pageable) {
        Page<LawyerProfile> lawyers = lawyerReader.findAllByVerificationStatus(
                VerificationStatus.VERIFIED, pageable);

        List<UUID> userIds = lawyers.getContent().stream()
                .map(LawyerProfile::getUserId).toList();
        Map<UUID, User> userMap = userReader.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<MatchingResponse> page = lawyers.map(lawyer -> {
            User user = userMap.get(lawyer.getUserId());
            List<String> matched = findMatchedKeywords(briefKeywords, lawyer.getTags());
            double score = briefKeywords.isEmpty() ? 0.0
                    : (double) matched.size() / briefKeywords.size();
            return new MatchingResponse(
                    lawyer.getUserId(),
                    user != null ? user.getName() : "알 수 없음",
                    user != null ? user.getProfileImageUrl() : null,
                    lawyer.getDomains(),
                    lawyer.getSubDomains(),
                    lawyer.getExperienceYears(),
                    lawyer.getTags(),
                    matched,
                    lawyer.getBio(),
                    lawyer.getRegion(),
                    score
            );
        });
        return PageResponse.from(page);
    }

    private List<String> findMatchedKeywords(List<String> briefKeywords, List<String> lawyerTags) {
        if (briefKeywords == null || lawyerTags == null) return Collections.emptyList();
        List<String> matched = new ArrayList<>(briefKeywords);
        matched.retainAll(lawyerTags);
        return matched;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.debug("jsonb 파싱 실패 raw={} error={}", json, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * {@code float[]} 를 pgvector 리터럴 {@code "[0.123456,...]"} 로 변환.
     * {@code PgLegalRetrievalService.floatArrayToPgVector} 와 동일 포맷.
     */
    static String floatArrayToPgVector(float[] vec) {
        StringBuilder sb = new StringBuilder(vec.length * 10 + 2);
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
