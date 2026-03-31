package org.example.shield.auth.application;

/**
 * Google OAuth 서비스 - Google API와 통신하여 사용자 정보를 받아온다.
 *
 * Layer: application
 * Called by: AuthService.googleLogin()
 * Calls: GoogleOAuthClient
 *
 * TODO:
 * - getUserInfo(authorizationCode):
 *   1. GoogleOAuthClient로 인증코드 → Access Token 교환
 *   2. Access Token으로 Google 사용자 정보 API 호출
 *   3. email, name 추출하여 반환
 */
public class GoogleOAuthService {
}
