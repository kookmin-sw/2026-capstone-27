package org.example.shield.ai.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 온톨로지 설정 — 슬림 JSON 로딩 및 분류 관련 설정.
 */
@Configuration
@Getter
@Slf4j
public class OntologyConfig {

    @Value("${ontology.slim-json-path:classpath:ontology/legal-ontology-slim.json}")
    private String slimJsonPath;

    @Value("${ontology.cache-ttl-minutes:60}")
    private int cacheTtlMinutes;

    private final ResourceLoader resourceLoader;

    public OntologyConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 슬림 온톨로지 JSON을 classpath에서 로드하여 String Bean으로 등록.
     * 앱 시작 시 1회 로딩 → 인메모리 캐싱.
     */
    @Bean
    public String slimOntologyJson() {
        try {
            Resource resource = resourceLoader.getResource(slimJsonPath);
            String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.info("슬림 온톨로지 JSON 로드 완료: {} chars", json.length());
            return json;
        } catch (IOException e) {
            log.error("슬림 온톨로지 JSON 로드 실패: {}", slimJsonPath, e);
            throw new RuntimeException("슬림 온톨로지 JSON 로드 실패: " + slimJsonPath, e);
        }
    }
}
