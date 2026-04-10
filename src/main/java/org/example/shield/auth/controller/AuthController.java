package org.example.shield.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.auth.application.AuthService;
import org.example.shield.auth.controller.dto.DevLoginRequest;
import org.example.shield.auth.domain.JwtToken;
import org.example.shield.auth.controller.dto.GoogleLoginRequest;
import org.example.shield.auth.controller.dto.LoginResponse;
import org.example.shield.auth.exception.InvalidTokenException;
import org.example.shield.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Google 로그인", description = "Google OAuth 인증 코드로 로그인 및 JWT 발급")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult result = authService.googleLogin(
                request.authorizationCode(), request.role());

        addRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", result.response()));
    }

    @Operation(summary = "개발용 로그인", description = "Google OAuth 없이 테스트용 JWT 발급")
    @PostMapping("/dev/login")
    public ResponseEntity<ApiResponse<LoginResponse>> devLogin(
            @RequestBody DevLoginRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult result = authService.devLogin(
                request.email(), request.name(), request.role());

        addRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("개발용 로그인 성공", result.response()));
    }

    @Operation(summary = "로그아웃", description = "현재 세션 종료 및 Refresh Token 무효화")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UUID userId,
            HttpServletResponse response) {
        authService.logout(userId);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token + Refresh Token 재발급")
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new InvalidTokenException();
        }
        JwtToken tokenPair = authService.refreshToken(refreshToken);
        addRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", tokenPair.accessToken()));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
