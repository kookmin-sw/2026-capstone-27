package org.example.shield.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record DevLoginRequest(
        @NotBlank String email,
        @NotBlank String name,
        @NotBlank String role
) {}
