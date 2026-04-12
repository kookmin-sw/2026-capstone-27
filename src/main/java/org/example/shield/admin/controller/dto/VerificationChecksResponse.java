package org.example.shield.admin.controller.dto;

import org.example.shield.admin.domain.VerificationCheck;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationChecksResponse(
        UUID lawyerId,
        boolean emailDuplicate,
        boolean phoneDuplicate,
        boolean nameDuplicate,
        boolean requiredFields,
        boolean licenseVerified,
        boolean documentMatched,
        boolean specializationValid,
        boolean experienceVerified,
        boolean duplicateSignup,
        boolean documentComplete,
        int completedCount,
        int totalCount,
        LocalDateTime updatedAt
) {
    public static VerificationChecksResponse from(VerificationCheck check) {
        return new VerificationChecksResponse(
                check.getLawyerId(),
                check.isEmailDuplicate(),
                check.isPhoneDuplicate(),
                check.isNameDuplicate(),
                check.isRequiredFields(),
                check.isLicenseVerified(),
                check.isDocumentMatched(),
                check.isSpecializationValid(),
                check.isExperienceVerified(),
                check.isDuplicateSignup(),
                check.isDocumentComplete(),
                check.getCompletedCount(),
                check.getTotalCount(),
                check.getUpdatedAt()
        );
    }

    public static VerificationChecksResponse empty(UUID lawyerId) {
        return new VerificationChecksResponse(
                lawyerId,
                false, false, false, false,
                false, false, false, false,
                false, false,
                0, 10,
                null
        );
    }
}
