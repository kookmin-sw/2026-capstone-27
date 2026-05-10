package org.example.shield.admin.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
        @NotBlank(message = "상태는 필수입니다")
        String status,
        String reason
) {}
