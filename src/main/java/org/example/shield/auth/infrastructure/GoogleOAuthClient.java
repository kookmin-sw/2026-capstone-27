package org.example.shield.auth.infrastructure;

/**
 * Google OAuth HTTP 클라이언트.
 *
 * Layer: infrastructure
 * Called by: GoogleOAuthService
 * Calls: Google OAuth API (https://oauth2.googleapis.com/token, https://www.googleapis.com/oauth2/v2/userinfo)
 *
 * TODO:
 * - exchangeCodeForToken(authorizationCode): 인증코드 → Google Access Token 교환
 * - getUserInfo(googleAccessToken): Google Access Token → 사용자 정보(email, name) 조회
 * - application.yml의 google.client-id, client-secret 사용
 */
public class GoogleOAuthClient {
}
