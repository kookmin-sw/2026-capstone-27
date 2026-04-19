package org.example.shield.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * JPA Auditing용 {@link DateTimeProvider} 빈.
 *
 * <p>Spring Data JPA Auditing은 기본적으로 {@link java.time.LocalDateTime}만 제공한다.
 * 일부 엔티티({@code LegalChunkEntity})가 {@link OffsetDateTime}을 사용하므로
 * 이에 대응하기 위해 명시적 provider를 등록한다.</p>
 *
 * <p>{@code OffsetDateTime.now()}를 반환하면 Spring Data는 대상 필드 타입
 * (LocalDateTime, OffsetDateTime 등)에 맞게 자동 변환한다.</p>
 */
@Configuration
public class AuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
