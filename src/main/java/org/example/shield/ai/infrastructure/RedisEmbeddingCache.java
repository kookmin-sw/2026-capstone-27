package org.example.shield.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.EmbeddingCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Redis 기반 쿼리 임베딩 캐시 (Phase B-5).
 *
 * <p>활성화 조건: {@code spring.data.redis.host}가 설정되고
 * {@code rag.cache.embedding.enabled=true} (기본) 일 때.</p>
 *
 * <p>키: {@code emb:{model}:{sha256(query)}}. query를 그대로 키에 넣지 않는 이유는
 * (1) 한글/특수문자 이스케이핑 회피, (2) 키 길이 상한 안정화, (3) 한 쿼리당 고정 44바이트 해시.</p>
 *
 * <p>값: JSON 배열 문자열 {@code "[0.1,-0.2,...]"}. pgvector 리터럴과 유사하지만
 * 파싱은 Jackson으로 처리한다. 바이너리 인코딩(float32 little-endian) 대신 JSON을
 * 선택한 이유는 redis-cli / Redis Insight로 직접 검증 가능한 가독성 때문이다.</p>
 *
 * <p>실패 정책: Redis 일시 장애가 RAG 전체를 막지 않도록 get/put 모두 예외를 삼키고
 * {@link Optional#empty()}를 반환한다. Cohere fallback 경로로 자연스럽게 흐른다.</p>
 */
@Component("redisEmbeddingCache")
@ConditionalOnExpression(
        "'${spring.data.redis.host:}' != '' && " +
        "'${rag.cache.embedding.enabled:true}' == 'true'")
@Slf4j
public class RedisEmbeddingCache implements EmbeddingCache {

    private static final String KEY_PREFIX = "emb:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisEmbeddingCache(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${rag.cache.embedding.ttl-seconds:86400}") long ttlSeconds) {
        this.redis = new StringRedisTemplate(connectionFactory);
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(Math.max(60L, ttlSeconds));
        log.info("EmbeddingCache = Redis (ttl={}s)", this.ttl.toSeconds());
    }

    @Override
    public Optional<float[]> get(String model, String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        String key = buildKey(model, query);
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            float[] vec = objectMapper.readValue(json, float[].class);
            return (vec == null || vec.length == 0) ? Optional.empty() : Optional.of(vec);
        } catch (Exception e) {
            log.warn("Redis 임베딩 캐시 조회 실패, miss 처리: key={}, error={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String model, String query, float[] embedding) {
        if (query == null || query.isBlank()) return;
        if (embedding == null || embedding.length == 0) return;
        String key = buildKey(model, query);
        try {
            String json = objectMapper.writeValueAsString(embedding);
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Redis 임베딩 캐시 저장 실패: key={}, error={}", key, e.getMessage());
        }
    }

    static String buildKey(String model, String query) {
        String modelPart = (model == null || model.isBlank()) ? "_" : model;
        String normalized = query.trim();
        String hash = sha256Hex(normalized);
        return KEY_PREFIX + modelPart + ":" + hash;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a required JDK algorithm; should never happen.
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }
}
