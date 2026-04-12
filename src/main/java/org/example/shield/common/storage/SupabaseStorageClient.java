package org.example.shield.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Slf4j
@Component
public class SupabaseStorageClient implements StorageClient {

    private final WebClient webClient;
    private final String bucket;
    private final String baseUrl;

    public SupabaseStorageClient(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket}") String bucket) {
        this.bucket = bucket;
        this.baseUrl = supabaseUrl + "/storage/v1";
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    @Override
    public String upload(String path, MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            webClient.post()
                    .uri(URI.create(baseUrl + "/object/" + bucket + "/" + path))
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header("x-upsert", "true")
                    .bodyValue(fileBytes)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("파일 업로드 완료. bucket={}, path={}", bucket, path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패", e);
        }
    }

    @Override
    public void delete(String path) {
        webClient.delete()
                .uri(URI.create(baseUrl + "/object/" + bucket + "/" + path))
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("파일 삭제 완료. bucket={}, path={}", bucket, path);
    }

    @Override
    public String getSignedUrl(String path, int expiresInSeconds) {
        Map<String, Object> response = webClient.post()
                .uri(URI.create(baseUrl + "/object/sign/" + bucket + "/" + path))
                .bodyValue(Map.of("expiresIn", expiresInSeconds))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String signedUrl = (String) response.get("signedURL");
        return baseUrl + signedUrl;
    }
}
