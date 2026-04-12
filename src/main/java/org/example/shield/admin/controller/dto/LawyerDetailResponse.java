package org.example.shield.admin.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LawyerDetailResponse(
        UUID lawyerId,
        UUID userId,
        String name,
        String email,
        String phone,
        String specializations,
        Integer experienceYears,
        List<String> certifications,
        String barAssociationNumber,
        String verificationStatus,
        String region,
        String bio,
        Integer caseCount,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static LawyerDetailResponse from(LawyerProfile lawyer, User user) {
        return new LawyerDetailResponse(
                lawyer.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                lawyer.getSpecializations(),
                lawyer.getExperienceYears(),
                lawyer.getCertifications(),
                lawyer.getBarAssociationNumber(),
                lawyer.getVerificationStatus().name(),
                lawyer.getRegion(),
                lawyer.getBio(),
                lawyer.getCaseCount(),
                lawyer.getTags(),
                lawyer.getCreatedAt()
        );
    }
}
