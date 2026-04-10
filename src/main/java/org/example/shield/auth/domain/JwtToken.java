package org.example.shield.auth.domain;

public record JwtToken(
        String accessToken,
        String refreshToken
) {}
