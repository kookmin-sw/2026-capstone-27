package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.controller.dto.LawyerResponse;
import org.example.shield.lawyer.controller.dto.ProfileUpdateRequest;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerService {

    private final LawyerReader lawyerReader;
    private final LawyerWriter lawyerWriter;
    private final UserReader userReader;

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

    @Transactional
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
        User user = userReader.findById(userId);
        return LawyerResponse.from(profile, user.getName(), user.getProfileImageUrl());
    }
}
