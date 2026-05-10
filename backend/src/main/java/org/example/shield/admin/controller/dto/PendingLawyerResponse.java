package org.example.shield.admin.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PendingLawyerResponse(
        UUID lawyerId,
        String name,
        String email,
        String phone,
        List<String> domains,
        List<String> subDomains,
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
                lawyer.getDomains(),
                lawyer.getSubDomains(),
                lawyer.getExperienceYears(),
                lawyer.getVerificationStatus().name(),
                documentCount,
                lawyer.getCreatedAt()
        );
    }
}
