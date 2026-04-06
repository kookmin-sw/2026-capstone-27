package org.example.shield.auth.domain;

public interface OAuthClient {

    OAuthUserInfo getUserInfo(String token);
}
