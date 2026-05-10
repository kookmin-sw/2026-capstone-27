package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.controller.dto.LawyerResponse;
import org.example.shield.lawyer.controller.dto.ProfileUpdateRequest;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.example.shield.common.config.RedisConfig.CACHE_LAWYER_RECOMMENDATIONS;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerService {

    private final LawyerReader lawyerReader;
    private final LawyerWriter lawyerWriter;
    private final UserReader userReader;
    private final LawyerEmbeddingService lawyerEmbeddingService;

    public PageResponse<LawyerResponse> getLawyers(Pageable pageable, String specialization, Integer minExperience) {
        Page<LawyerProfile> profiles = lawyerReader.findVerifiedLawyers(
                specialization, minExperience, pageable);

        List<UUID> userIds = profiles.getContent().stream()
                .map(LawyerProfile::getUserId)
                .toList();
        Map<UUID, User> userMap = userReader.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Page<LawyerResponse> responsePage = profiles.map(profile -> {
            User user = userMap.get(profile.getUserId());
            return LawyerResponse.from(profile,
                    user != null ? user.getName() : null,
                    user != null ? user.getProfileImageUrl() : null);
        });
        return PageResponse.from(responsePage);
    }

    public LawyerResponse getLawyer(UUID userId) {
        LawyerProfile profile = lawyerReader.findByUserId(userId);
        User user = userReader.findById(profile.getUserId());
        return LawyerResponse.from(profile, user.getName(), user.getProfileImageUrl());
    }

    public LawyerResponse getMyProfile(UUID userId) {
        LawyerProfile profile = lawyerReader.findByUserId(userId);
        User user = userReader.findById(userId);
        return LawyerResponse.from(profile, user.getName(), user.getProfileImageUrl());
    }

    /**
     * 변호사 프로필 수정. 변경된 필드(분야/태그/소개 등)는 추천 결과에 영향을 주므로
     * 전체 추천 캐시를 무효화한다 (Issue #76 Phase 3).
     */
    @Transactional
    @CacheEvict(value = CACHE_LAWYER_RECOMMENDATIONS, allEntries = true)
    public LawyerResponse updateMyProfile(UUID userId, ProfileUpdateRequest request) {
        LawyerProfile profile = lawyerReader.findByUserId(userId);
        profile.updateProfile(
                request.domains(),
                request.subDomains(),
                request.experienceYears(),
                request.certifications(),
                request.tags(),
                request.bio(),
                request.region()
        );

        // VERIFIED 변호사 가 프로필 을 바꾸면 임베딩을 새로 계산 (Issue #50)
        if (profile.getVerificationStatus() == VerificationStatus.VERIFIED) {
            try {
                lawyerEmbeddingService.upsertEmbedding(profile);
            } catch (Exception ex) {
                log.warn("변호사 임베딩 재계산 실패 (프로필 저장은 성공) lawyerId={} error={}",
                        profile.getId(), ex.getMessage());
            }
        }

        User user = userReader.findById(userId);
        return LawyerResponse.from(profile, user.getName(), user.getProfileImageUrl());
    }
}
