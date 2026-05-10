package org.example.shield.auth.application;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.shield.auth.domain.JwtToken;
import org.example.shield.auth.infrastructure.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
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

    /**
     * 토큰의 잔여 만료 시간을 반환한다 (현재 시각 기준).
     * 만료된 토큰이면 {@link Duration#ZERO}.
     * 블랙리스트 TTL 산정용 (Issue #80).
     */
    public Duration getRemainingTtl(String token) {
        Claims claims = jwtTokenProvider.parseClaims(token);
        Instant expiration = claims.getExpiration().toInstant();
        Duration remaining = Duration.between(Instant.now(), expiration);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
