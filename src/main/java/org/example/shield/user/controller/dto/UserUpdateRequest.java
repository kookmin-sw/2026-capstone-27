package org.example.shield.user.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank(message = "이름은 필수입니다")
        String name
) {}
