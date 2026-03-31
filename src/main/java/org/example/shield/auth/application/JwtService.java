package org.example.shield.auth.application;

/**
 * JWT 서비스 - JWT 토큰 생성/검증/파싱 로직.
 *
 * Layer: application
 * Called by: AuthService, JwtAuthFilter
 * Calls: JwtTokenProvider
 *
 * TODO:
 * - createTokenPair(userId, role): Access Token(30분) + Refresh Token(14일) 생성
 * - validateToken(token): 토큰 유효성 검증 (만료, 위조, 블랙리스트)
 * - getUserIdFromToken(token): 토큰에서 userId 추출
 * - getRoleFromToken(token): 토큰에서 role 추출
 */
public class JwtService {
}
