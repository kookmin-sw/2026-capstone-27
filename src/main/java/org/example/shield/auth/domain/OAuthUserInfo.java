package org.example.shield.auth.domain;

public record OAuthUserInfo(
        String email,
        String name,
        String googleId
) {}
