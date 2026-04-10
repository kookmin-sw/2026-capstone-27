package org.example.shield.auth.controller.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String name,
        String role
) {}
