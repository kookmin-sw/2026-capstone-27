package org.example.shield.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.auth.application.AuthService;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult result = authService.googleLogin(
                request.authorizationCode(), request.role());

        addRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UUID userId,
            HttpServletResponse response) {
        authService.logout(userId);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidTokenException();
        }
        String newAccessToken = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", newAccessToken));
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
