package org.example.shield.auth.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.auth.domain.OAuthClient;
import org.example.shield.auth.domain.OAuthUserInfo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final OAuthClient oAuthClient;

    public OAuthUserInfo getUserInfo(String authorizationCode) {
        return oAuthClient.getUserInfo(authorizationCode);
    }
}
