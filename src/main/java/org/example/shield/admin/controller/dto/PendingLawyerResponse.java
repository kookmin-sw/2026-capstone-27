package org.example.shield.admin.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.user.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingLawyerResponse(
        UUID lawyerId,
        String name,
        String email,
        String phone,
        String specializations,
        Integer experienceYears,
        String verificationStatus,
        long documentCount,
        LocalDateTime createdAt
) {
    public static PendingLawyerResponse from(LawyerProfile lawyer, User user, long documentCount) {
        return new PendingLawyerResponse(
                lawyer.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                lawyer.getSpecializations(),
                lawyer.getExperienceYears(),
                lawyer.getVerificationStatus().name(),
                documentCount,
                lawyer.getCreatedAt()
        );
    }
}
