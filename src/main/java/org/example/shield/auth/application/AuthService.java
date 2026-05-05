package org.example.shield.auth.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.auth.controller.dto.LoginResponse;
import org.example.shield.auth.domain.JwtToken;
import org.example.shield.auth.domain.OAuthUserInfo;
import org.example.shield.auth.exception.InvalidRoleException;
import org.example.shield.auth.exception.InvalidTokenException;
import org.example.shield.common.enums.UserRole;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.user.domain.User;
import org.example.shield.user.domain.UserReader;
import org.example.shield.user.domain.UserWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final GoogleOAuthService googleOAuthService;
    private final JwtService jwtService;
    private final UserReader userReader;
    private final UserWriter userWriter;
    private final TokenBlacklistService tokenBlacklistService;

    public record LoginResult(LoginResponse response, String refreshToken) {}

    public LoginResult googleLogin(String authorizationCode, String role) {
        OAuthUserInfo userInfo = googleOAuthService.getUserInfo(authorizationCode);

        // 신규 가입 여부를 구분하기 위해 조회 시점을 명시적으로 분리한다.
        var existing = userReader.findByGoogleId(userInfo.googleId());
        boolean isNewUser = existing.isEmpty();

        // 보안: 신규 가입 시에는 클라이언트가 전달한 role 을 신뢰하지 않고
        // 항상 USER 로 고정한다. LAWYER/ADMIN 으로의 승격은 전용 에지 API
        // (예: /api/lawyers/me/register) 를 통해서만 일어나야 한다.
        // parseRole(role) 결과는 더 이상 사용하지 않으나, 입력 검증(InvalidRoleException) 목적으로 호출을 유지한다.
        if (role != null && !role.isBlank()) {
            parseRole(role);
        }

        User user = existing.orElseGet(() -> userWriter.save(
                User.builder()
                        .email(userInfo.email())
                        .name(userInfo.name())
                        .role(UserRole.USER)
                        .provider("GOOGLE")
                        .googleId(userInfo.googleId())
                        .build()
        ));

        JwtToken tokenPair = jwtService.createTokenPair(user.getId(), user.getRole().name());
        user.updateRefreshToken(tokenPair.refreshToken());

        LoginResponse response = new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                user.getId(),
                user.getName(),
                user.getRole().name(),
                isNewUser
        );
        return new LoginResult(response, tokenPair.refreshToken());
    }

    public LoginResult devLogin(String email, String name, String role) {
        User user = userReader.findByEmail(email)
                .orElseGet(() -> userWriter.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .role(parseRole(role))
                                .provider("DEV")
                                .googleId("dev-" + UUID.randomUUID())
                                .build()
                ));

        JwtToken tokenPair = jwtService.createTokenPair(user.getId(), user.getRole().name());
        user.updateRefreshToken(tokenPair.refreshToken());

        LoginResponse response = new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                user.getId(),
                user.getName(),
                user.getRole().name(),
                false
        );
        return new LoginResult(response, tokenPair.refreshToken());
    }

    /**
     * 로그아웃: 현재 Refresh Token 을 블랙리스트에 등록하고 DB 의 토큰을 비운다 (Issue #80).
     *
     * <p>블랙리스트 등록 후 즉시 모든 갱신 시도가 거부되므로, 단순 DB null 처리만 하던
     * 기존 동작 대비 토큰 탈취 시점부터의 강제 무효화 보안이 강화된다.</p>
     */
    public void logout(UUID userId) {
        User user = userReader.findById(userId);
        String currentToken = user.getRefreshToken();
        if (currentToken != null && !currentToken.isBlank()) {
            try {
                tokenBlacklistService.blacklist(
                        currentToken,
                        jwtService.getRemainingTtl(currentToken)
                );
            } catch (Exception e) {
                // 토큰 자체가 이미 만료되어 파싱 실패 등의 케이스는 무시 (어차피 무효 토큰)
            }
        }
        user.updateRefreshToken(null);
    }

    public JwtToken refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.TOKEN_EXPIRED);
        }

        // Issue #80: 블랙리스트 hit 이면 즉시 거부 (DB 검증 이전 단계에서 차단)
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.REFRESH_TOKEN_BLACKLISTED);
        }

        UUID userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userReader.findById(userId);

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        JwtToken tokenPair = jwtService.createTokenPair(userId, user.getRole().name());
        user.updateRefreshToken(tokenPair.refreshToken());

        return tokenPair;
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.USER;
        }
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException();
        }
    }
}
