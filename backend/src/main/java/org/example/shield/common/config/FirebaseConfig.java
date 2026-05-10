package org.example.shield.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // classpath: / file: prefix 모두 지원. prefix 없으면 classpath 로 처리하여 기존 동작 유지.
        String location = serviceAccountPath.contains(":")
                ? serviceAccountPath
                : "classpath:" + serviceAccountPath;

        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IOException("Firebase service account 파일을 찾을 수 없습니다: " + location);
        }

        try (InputStream is = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();
            log.info("Firebase 초기화 성공. location={}", location);
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}