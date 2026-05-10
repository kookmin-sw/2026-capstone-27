package org.example.shield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableCaching
public class ShieldApplication {

    public static void main(String[] args) {
        // EC2(UTC) 환경에서도 모든 시각이 KST 기준으로 일관되도록 JVM 기본 타임존을 강제.
        // Hibernate jdbc.time_zone 및 Jackson time-zone 설정과 함께 작동하여
        // LocalDateTime 저장/직렬화 시 Asia/Seoul 오프셋(+09:00)을 사용하게 한다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.setProperty("user.timezone", "Asia/Seoul");

        SpringApplication.run(ShieldApplication.class, args);
    }

}
