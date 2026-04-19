package org.example.shield.ai.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisEmbeddingCache#buildKey(String, String)}의 키 생성 규칙 검증.
 *
 * <p>실제 Redis 연결은 요구하지 않는 순수 해시 단위 테스트.</p>
 */
class RedisEmbeddingCacheKeyTest {

    @Test
    @DisplayName("buildKey — 동일 (model, query) 입력은 동일 키를 생성한다")
    void sameInputsYieldSameKey() {
        String k1 = RedisEmbeddingCache.buildKey("embed-v4.0", "전세 보증금");
        String k2 = RedisEmbeddingCache.buildKey("embed-v4.0", "전세 보증금");
        assertThat(k1).isEqualTo(k2);
    }

    @Test
    @DisplayName("buildKey — 모델이 다르면 키가 달라진다")
    void differentModelYieldsDifferentKey() {
        String k1 = RedisEmbeddingCache.buildKey("embed-v4.0", "전세 보증금");
        String k2 = RedisEmbeddingCache.buildKey("embed-v3.0", "전세 보증금");
        assertThat(k1).isNotEqualTo(k2);
    }

    @Test
    @DisplayName("buildKey — 쿼리 양쪽 공백만 다른 경우 같은 키로 정규화된다")
    void trimsWhitespace() {
        String k1 = RedisEmbeddingCache.buildKey("embed-v4.0", "임대차 해지");
        String k2 = RedisEmbeddingCache.buildKey("embed-v4.0", "  임대차 해지  ");
        assertThat(k1).isEqualTo(k2);
    }

    @Test
    @DisplayName("buildKey — 접두어 emb: + 모델명 + 64자리 hex SHA-256 형식")
    void keyFormat() {
        String k = RedisEmbeddingCache.buildKey("embed-v4.0", "소유권 이전");
        assertThat(k).startsWith("emb:embed-v4.0:");
        String hash = k.substring("emb:embed-v4.0:".length());
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("buildKey — null/blank 모델은 '_' placeholder로 치환")
    void nullModelFallback() {
        String k1 = RedisEmbeddingCache.buildKey(null, "소유권");
        String k2 = RedisEmbeddingCache.buildKey("", "소유권");
        assertThat(k1).startsWith("emb:_:");
        assertThat(k2).startsWith("emb:_:");
        assertThat(k1).isEqualTo(k2);
    }
}
