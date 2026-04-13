package org.example.shield.lawyer.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
        @NotBlank(message = "대한변호사협회 등록번호는 필수입니다")
        String barAssociationNumber
) {}
