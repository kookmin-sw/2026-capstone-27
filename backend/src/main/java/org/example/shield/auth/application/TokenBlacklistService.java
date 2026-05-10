package org.example.shield.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Refresh Token 블랙리스트 서비스 (Issue #80).
 *
 * <p>로그아웃 시 Refresh Token 을 즉시 무효화하기 위해 Redis 에 토큰 해시를 저장한다.
 * Refresh Token 갱신 요청이 들어올 때마다 블랙리스트를 조회해 hit 이면 거부한다.</p>
 *
 * <p>저장 형태:
 * <ul>
 *   <li>Key: {@code blacklist::refresh::{sha256(token)}}</li>
 *   <li>Value: 마커 문자열 (실질 정보 없음, 존재 여부만 확인)</li>
 *   <li>TTL: 토큰의 잔여 만료 시간 (만료 후엔 어차피 무의미하므로 자동 정리)</li>
 * </ul>
 *
 * <p>토큰 평문이 아닌 SHA-256 해시로 저장하여 Redis 가 노출돼도 토큰이 유출되지 않도록 한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist::refresh::";
    private static final String MARKER_VALUE = "1";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Refresh Token 을 블랙리스트에 등록한다.
     *
     * @param token 평문 Refresh Token
     * @param ttl   유지 시간 (보통 토큰 잔여 만료 시간). 0 이하면 등록 생략.
     */
    public void blacklist(String token, Duration ttl) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            log.debug("블랙리스트 등록 skip: TTL 0 이하");
            return;
        }
        String key = buildKey(token);
        redisTemplate.opsForValue().set(key, MARKER_VALUE, ttl);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인한다.
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = buildKey(token);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    private String buildKey(String token) {
        return KEY_PREFIX + sha256Hex(token);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }
}
