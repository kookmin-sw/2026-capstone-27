package org.example.shield.auth.infrastructure;

/**
 * JWT 토큰 생성/파싱 유틸.
 *
 * Layer: infrastructure
 * Called by: JwtService
 *
 * TODO:
 * - generateToken(userId, role, expiry): JWT 문자열 생성
 * - parseClaims(token): JWT에서 Claims 추출
 * - isExpired(token): 만료 여부 확인
 * - application.yml의 jwt.secret, jwt.access-token-expiry 사용
 */
public class JwtTokenProvider {
}
