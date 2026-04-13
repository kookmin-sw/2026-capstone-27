package org.example.shield.lawyer.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;

import java.time.LocalDateTime;

public record VerificationResponse(
        String verificationStatus,
        String barAssociationNumber,
        LocalDateTime verifiedAt
) {
    public static VerificationResponse from(LawyerProfile profile) {
        return new VerificationResponse(
                profile.getVerificationStatus().name(),
                profile.getBarAssociationNumber(),
                profile.getVerifiedAt()
        );
    }
}
