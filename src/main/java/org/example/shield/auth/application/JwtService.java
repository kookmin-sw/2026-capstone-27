package org.example.shield.auth.application;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.shield.auth.domain.JwtToken;
import org.example.shield.auth.infrastructure.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtToken createTokenPair(UUID userId, String role) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, role);
        return new JwtToken(accessToken, refreshToken);
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.isValid(token);
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = jwtTokenProvider.parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = jwtTokenProvider.parseClaims(token);
        return claims.get("role", String.class);
    }
}
