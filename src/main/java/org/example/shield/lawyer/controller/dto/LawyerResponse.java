package org.example.shield.lawyer.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;

import java.util.List;
import java.util.UUID;

public record LawyerResponse(
        UUID lawyerId,
        String name,
        String profileImageUrl,
        List<String> domains,
        List<String> subDomains,
        Integer experienceYears,
        List<String> tags,
        List<String> certifications,
        Integer caseCount,
        String bio,
        String region,
        String verificationStatus
) {
    public static LawyerResponse from(LawyerProfile profile, String name, String profileImageUrl) {
        return new LawyerResponse(
                profile.getId(),
                name,
                profileImageUrl,
                profile.getDomains(),
                profile.getSubDomains(),
                profile.getExperienceYears(),
                profile.getTags(),
                profile.getCertifications(),
                profile.getCaseCount(),
                profile.getBio(),
                profile.getRegion(),
                profile.getVerificationStatus().name()
        );
    }
}
