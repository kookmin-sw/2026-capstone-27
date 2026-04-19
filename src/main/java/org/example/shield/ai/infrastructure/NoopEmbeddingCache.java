package org.example.shield.ai.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.example.shield.ai.application.EmbeddingCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 쿼리 임베딩 캐시의 no-op 구현 (Phase B-5).
 *
 * <p>Redis 연결이 설정되지 않았거나 {@code rag.cache.embedding.enabled=false} 상태에서
 * 기본 주입된다. {@link RedisEmbeddingCache}가 활성화되면 그쪽이 우선 주입되도록
 * {@link ConditionalOnMissingBean}을 사용한다.</p>
 */
@Component
@ConditionalOnMissingBean(name = "redisEmbeddingCache")
@ConditionalOnProperty(name = "rag.cache.embedding.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class NoopEmbeddingCache implements EmbeddingCache {

    public NoopEmbeddingCache() {
        log.info("EmbeddingCache = Noop (Redis 미구성 또는 비활성화)");
    }

    @Override
    public Optional<float[]> get(String model, String query) {
        return Optional.empty();
    }

    @Override
    public void put(String model, String query, float[] embedding) {
        // no-op
    }
}
