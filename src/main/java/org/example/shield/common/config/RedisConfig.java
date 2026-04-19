package org.example.shield.common.config;

/**
 * Redis 설정 placeholder.
 *
 * <p>Spring Boot 4.0 + {@code spring-boot-starter-data-redis}의 auto-configuration을 그대로 사용한다.
 * 연결 정보는 {@code spring.data.redis.host/port/password}로 주입되며, 미지정 시
 * {@link org.example.shield.ai.infrastructure.RedisEmbeddingCache}가 등록되지 않고
 * {@link org.example.shield.ai.infrastructure.NoopEmbeddingCache}가 주입된다.</p>
 *
 * <p>별도의 {@code @Configuration}이 필요할 경우(예: 커스텀 직렬화) 이 클래스를
 * 확장할 여지를 남긴다.</p>
 */
public class RedisConfig {
}
