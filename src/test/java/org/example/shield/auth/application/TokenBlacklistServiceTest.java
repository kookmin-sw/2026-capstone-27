package org.example.shield.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue #80 — Refresh Token 블랙리스트 서비스 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService(redisTemplate);
    }

    @Test
    @DisplayName("토큰을 블랙리스트에 등록하면 SHA-256 해시 키로 TTL 과 함께 저장된다")
    void blacklist_storesHashedKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = "eyJhbGci.someRefreshToken";
        Duration ttl = Duration.ofMinutes(30);

        service.blacklist(token, ttl);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCap.capture(), valCap.capture(), ttlCap.capture());

        // 키는 prefix + 64자 hex (SHA-256)
        assertThat(keyCap.getValue()).startsWith("blacklist::refresh::");
        assertThat(keyCap.getValue().substring("blacklist::refresh::".length()))
                .hasSize(64)
                .matches("[0-9a-f]+");
        assertThat(ttlCap.getValue()).isEqualTo(ttl);
    }

    @Test
    @DisplayName("TTL 이 0 이하면 등록을 생략한다")
    void blacklist_skipsWhenTtlIsZeroOrNegative() {
        service.blacklist("token", Duration.ZERO);
        service.blacklist("token", Duration.ofSeconds(-10));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("토큰이 null/blank 면 등록을 생략한다")
    void blacklist_skipsWhenTokenIsBlank() {
        service.blacklist(null, Duration.ofMinutes(10));
        service.blacklist("", Duration.ofMinutes(10));
        service.blacklist("   ", Duration.ofMinutes(10));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("같은 토큰을 두 번 hash 하면 동일한 키가 나온다 (결정적 해시)")
    void blacklist_sameTokenProducesSameKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = "same-token";
        service.blacklist(token, Duration.ofMinutes(1));
        service.blacklist(token, Duration.ofMinutes(2));

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, org.mockito.Mockito.times(2))
                .set(keyCap.capture(), any(), any(Duration.class));

        assertThat(keyCap.getAllValues()).hasSize(2);
        assertThat(keyCap.getAllValues().get(0)).isEqualTo(keyCap.getAllValues().get(1));
    }

    @Test
    @DisplayName("isBlacklisted: Redis 에 키가 있으면 true")
    void isBlacklisted_trueWhenKeyExists() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = service.isBlacklisted("token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted: Redis 에 키가 없으면 false")
    void isBlacklisted_falseWhenKeyMissing() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        boolean result = service.isBlacklisted("token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlacklisted: hasKey 가 null 반환해도 안전하게 false")
    void isBlacklisted_falseWhenHasKeyReturnsNull() {
        when(redisTemplate.hasKey(anyString())).thenReturn(null);

        boolean result = service.isBlacklisted("token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlacklisted: 토큰이 null/blank 면 Redis 조회 없이 false")
    void isBlacklisted_falseWhenTokenBlank() {
        assertThat(service.isBlacklisted(null)).isFalse();
        assertThat(service.isBlacklisted("")).isFalse();

        verify(redisTemplate, never()).hasKey(anyString());
    }
}
