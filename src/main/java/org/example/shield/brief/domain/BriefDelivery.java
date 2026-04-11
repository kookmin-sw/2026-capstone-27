package org.example.shield.brief.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.enums.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BriefDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID briefId;

    @Column(nullable = false)
    private UUID lawyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "delivery_status")
    private DeliveryStatus status;

    @Column(columnDefinition = "text")
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private LocalDateTime viewedAt;

    private LocalDateTime respondedAt;

    @Builder
    private BriefDelivery(UUID briefId, UUID lawyerId) {
        this.briefId = briefId;
        this.lawyerId = lawyerId;
        this.status = DeliveryStatus.DELIVERED;
        this.sentAt = LocalDateTime.now();
    }

    public static BriefDelivery create(UUID briefId, UUID lawyerId) {
        return BriefDelivery.builder()
                .briefId(briefId)
                .lawyerId(lawyerId)
                .build();
    }

    public void markViewed() {
        if (this.viewedAt == null) {
            this.viewedAt = LocalDateTime.now();
        }
    }

    public void accept() {
        this.status = DeliveryStatus.CONFIRMED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = DeliveryStatus.REJECTED;
        this.rejectionReason = reason;
        this.respondedAt = LocalDateTime.now();
    }
}
