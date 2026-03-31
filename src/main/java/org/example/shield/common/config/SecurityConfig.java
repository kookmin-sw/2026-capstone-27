package org.example.shield.common.config;

/**
 * Spring Security 설정.
 *
 * TODO: @Configuration + @EnableWebSecurity + @EnableMethodSecurity
 * - CORS 설정 (localhost:3000 허용)
 * - CSRF 비활성화 (JWT 사용이므로)
 * - Stateless 세션
 * - 공개 경로: /api/auth/**, /swagger-ui/**, /api-docs/**, /v3/api-docs/**
 * - JwtAuthFilter를 UsernamePasswordAuthenticationFilter 전에 등록
 */
public class SecurityConfig {
}
