package org.example.shield.auth.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.auth.domain.OAuthClient;
import org.example.shield.auth.domain.OAuthUserInfo;
import org.example.shield.auth.exception.OAuthFailedException;
import org.example.shield.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class GoogleOAuthClient implements OAuthClient {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthClient(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret}") String clientSecret,
            @Value("${google.redirect-uri}") String redirectUri) {
        this.webClient = WebClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public OAuthUserInfo getUserInfo(String authorizationCode) {
        String accessToken = exchangeCodeForToken(authorizationCode);
        return fetchUserInfo(accessToken);
    }

    private String exchangeCodeForToken(String authorizationCode) {
        try {
            GoogleTokenResponse response = webClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("code=" + authorizationCode
                            + "&client_id=" + clientId
                            + "&client_secret=" + clientSecret
                            + "&redirect_uri=" + redirectUri
                            + "&grant_type=authorization_code")
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();

            if (response == null || response.accessToken() == null) {
                throw new OAuthFailedException(ErrorCode.OAUTH_CODE_INVALID);
            }
            return response.accessToken();
        } catch (OAuthFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google OAuth token exchange failed", e);
            throw new OAuthFailedException(ErrorCode.OAUTH_CODE_INVALID);
        }
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        try {
            GoogleUserResponse response = webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserResponse.class)
                    .block();

            if (response == null || response.email() == null) {
                throw new OAuthFailedException(ErrorCode.OAUTH_USER_INFO_FAILED);
            }
            return new OAuthUserInfo(response.email(), response.name(), response.id());
        } catch (OAuthFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google OAuth user info fetch failed", e);
            throw new OAuthFailedException(ErrorCode.OAUTH_USER_INFO_FAILED);
        }
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType
    ) {}

    private record GoogleUserResponse(
            String id,
            String email,
            String name,
            String picture
    ) {}
}
