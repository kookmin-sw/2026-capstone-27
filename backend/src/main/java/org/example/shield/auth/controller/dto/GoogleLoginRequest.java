package org.example.shield.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "인증 코드는 필수입니다")
        String authorizationCode,

        String role
) {}
