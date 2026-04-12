package org.example.shield.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID lawyerId;

    @Column(nullable = false)
    private UUID adminId;

    @Column(nullable = false, length = 30)
    private String fromStatus;

    @Column(nullable = false, length = 30)
    private String toStatus;

    @Column(columnDefinition = "text")
    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private VerificationLog(UUID lawyerId, UUID adminId,
                            String fromStatus, String toStatus, String reason) {
        this.lawyerId = lawyerId;
        this.adminId = adminId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
    }

    public static VerificationLog create(UUID lawyerId, UUID adminId,
                                         String fromStatus, String toStatus, String reason) {
        return VerificationLog.builder()
                .lawyerId(lawyerId)
                .adminId(adminId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .build();
    }
}
