package org.example.shield.lawyer.controller.dto;

import org.example.shield.lawyer.domain.LawyerProfile;

import java.time.LocalDateTime;

public record VerificationResponse(
        String verificationStatus,
        String barAssociationNumber,
        LocalDateTime requestedAt,
        LocalDateTime verifiedAt
) {
    public static VerificationResponse from(LawyerProfile profile) {
        return new VerificationResponse(
                profile.getVerificationStatus().name(),
                profile.getBarAssociationNumber(),
                // TODO: LawyerProfile에 verificationRequestedAt 필드 추가 후 매핑 변경
                profile.getCreatedAt(),
                profile.getVerifiedAt()
        );
    }
}
