package org.example.shield.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 설정 (Issue #76).
 *
 * <ul>
 *   <li>{@link RedisTemplate} : 직접 키/값 조작용 (예: Refresh Token 블랙리스트)</li>
 *   <li>{@link CacheManager} : 어노테이션 기반 캐시용 ({@code @Cacheable / @CacheEvict})</li>
 * </ul>
 *
 * <p>캐시별 TTL:
 * <ul>
 *   <li>{@code lawyer-recommendations} : 10분 — 변호사 풀 변동 빈도 낮음</li>
 *   <li>기본 : 5분</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    public static final String CACHE_LAWYER_RECOMMENDATIONS = "lawyer-recommendations";

    /**
     * 직접 사용용 RedisTemplate.
     * - Key: String
     * - Value: JSON 직렬화 (Jackson, Java Time 모듈 포함)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonRedisSerializer());
        template.setHashValueSerializer(jsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 어노테이션 기반 캐시용 CacheManager.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCacheConfigs = new HashMap<>();
        perCacheConfigs.put(CACHE_LAWYER_RECOMMENDATIONS,
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
    }

    /**
     * 다형 타입 정보를 포함한 Jackson 기반 Redis 직렬화기.
     *
     * <p>역직렬화 시 GenericJackson2JsonRedisSerializer 가 root 를 Object 로 읽기 때문에
     * 모든 객체(record 같은 final 포함)에 {@code @class} 가 필요하다.
     * 따라서 {@link ObjectMapper.DefaultTyping#EVERYTHING} 사용.
     * (NON_FINAL 사용 시 record 직렬화 후 역직렬화 단계에서 타입 정보 누락으로 실패함)</p>
     *
     * <p>다형 타입 안전을 위해 PolymorphicTypeValidator 화이트리스트 적용:
     * <ul>
     *   <li>{@code org.example.shield.*} — 애플리케이션 도메인/DTO</li>
     *   <li>{@code java.util.*} — 컬렉션</li>
     *   <li>{@code java.time.*} — LocalDateTime 등</li>
     *   <li>{@code org.springframework.data.domain.*} — PageImpl, Sort 등 페이지네이션</li>
     * </ul>
     */
    private GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType("org.example.shield")
                        .allowIfSubType("org.example.shield")
                        .allowIfBaseType("java.util.")
                        .allowIfSubType("java.util.")
                        .allowIfBaseType("java.time.")
                        .allowIfSubType("java.time.")
                        .allowIfBaseType("org.springframework.data.domain.")
                        .allowIfSubType("org.springframework.data.domain.")
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
