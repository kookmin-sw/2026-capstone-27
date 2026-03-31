package org.example.shield.auth.infrastructure;

/**
 * JWT 인증 필터 - 모든 요청에서 JWT를 검증하는 Spring Security 필터.
 *
 * Layer: infrastructure
 * Called by: Spring Security 필터 체인 (SecurityConfig에서 등록)
 * Calls: JwtService
 *
 * TODO:
 * - doFilterInternal(request, response, filterChain):
 *   1. Authorization Header에서 "Bearer {token}" 추출
 *   2. JwtService.validateToken(token)으로 검증
 *   3. 유효하면 SecurityContext에 인증 정보 설정
 *   4. 유효하지 않으면 401 반환
 *   5. /api/auth/**, /swagger-ui/** 경로는 필터 건너뛰기
 */
public class JwtAuthFilter {
}
