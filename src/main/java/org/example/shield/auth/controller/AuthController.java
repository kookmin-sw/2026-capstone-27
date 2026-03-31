package org.example.shield.auth.controller;

/**
 * 인증 API 컨트롤러.
 *
 * Layer: controller
 * Called by: 프론트엔드
 * Calls: AuthService
 *
 * API 목록 (3개):
 * - POST /api/auth/google    구글 로그인 (body: authorizationCode, role)
 * - POST /api/auth/logout     로그아웃 (Header: Authorization Bearer)
 * - POST /api/auth/token/refresh  토큰 갱신 (Cookie: refreshToken)
 */
public class AuthController {
}
