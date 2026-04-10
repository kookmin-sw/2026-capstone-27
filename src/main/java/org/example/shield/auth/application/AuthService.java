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

    public record LoginResult(LoginResponse response, String refreshToken) {}

    public LoginResult googleLogin(String authorizationCode, String role) {
        OAuthUserInfo userInfo = googleOAuthService.getUserInfo(authorizationCode);

        User user = userReader.findByGoogleId(userInfo.googleId())
                .orElseGet(() -> userWriter.save(
                        User.builder()
                                .email(userInfo.email())
                                .name(userInfo.name())
                                .role(parseRole(role))
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
                user.getRole().name()
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
                user.getRole().name()
        );
        return new LoginResult(response, tokenPair.refreshToken());
    }

    public void logout(UUID userId) {
        User user = userReader.findById(userId);
        user.updateRefreshToken(null);
    }

    public JwtToken refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.TOKEN_EXPIRED);
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
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException();
        }
    }
}
