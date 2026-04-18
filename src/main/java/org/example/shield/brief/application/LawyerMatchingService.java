package org.example.shield.brief.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.brief.controller.dto.MatchingResponse;
import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefReader;
import org.example.shield.brief.exception.BriefNotFoundException;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerMatchingService {

    private final BriefReader briefReader;
    private final LawyerReader lawyerReader;
    private final UserReader userReader;

    // TODO: AI 서버 연동 시 벡터DB 유사도 검색으로 교체
    // 현재는 VERIFIED 변호사를 경력순으로 반환하는 목 매칭
    public PageResponse<MatchingResponse> findMatching(UUID briefId, UUID userId, Pageable pageable) {
        Brief brief = briefReader.findById(briefId);
        if (!brief.getUserId().equals(userId)) {
            throw new BriefNotFoundException(briefId);
        }

        List<String> briefKeywords = brief.getKeywords() != null ? brief.getKeywords() : Collections.emptyList();

        Page<LawyerProfile> lawyers = lawyerReader.findAllByVerificationStatus(
                VerificationStatus.VERIFIED, pageable);

        // 변호사 userId 일괄 조회 (이름 가져오기용)
        List<UUID> userIds = lawyers.getContent().stream()
                .map(LawyerProfile::getUserId).toList();
        Map<UUID, User> userMap = userReader.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<MatchingResponse> responsePage = lawyers.map(lawyer -> {
            User user = userMap.get(lawyer.getUserId());
            List<String> matched = findMatchedKeywords(briefKeywords, lawyer.getTags());
            // TODO: AI 매칭 점수로 교체 — 현재는 키워드 일치 비율 기반 간이 스코어
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

        return PageResponse.from(responsePage);
    }

    private List<String> findMatchedKeywords(List<String> briefKeywords, List<String> lawyerTags) {
        if (briefKeywords == null || lawyerTags == null) return Collections.emptyList();
        List<String> matched = new ArrayList<>(briefKeywords);
        matched.retainAll(lawyerTags);
        return matched;
    }
}
