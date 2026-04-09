package org.example.shield.auth.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.auth.controller.dto.LoginResponse;
import org.example.shield.auth.domain.JwtToken;
import org.example.shield.auth.domain.OAuthUserInfo;
import org.example.shield.auth.exception.InvalidTokenException;
import org.example.shield.common.enums.UserRole;
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
                                .role(UserRole.valueOf(role))
                                .provider("GOOGLE")
                                .googleId(userInfo.googleId())
                                .build()
                ));

        JwtToken tokenPair = jwtService.createTokenPair(user.getId(), user.getRole().name());
        user.updateRefreshToken(tokenPair.refreshToken());

        LoginResponse response = new LoginResponse(
                tokenPair.accessToken(),
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

    public String refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        UUID userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userReader.findById(userId);

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new InvalidTokenException();
        }

        JwtToken tokenPair = jwtService.createTokenPair(userId, user.getRole().name());
        user.updateRefreshToken(tokenPair.refreshToken());

        return tokenPair.accessToken();
    }
}
