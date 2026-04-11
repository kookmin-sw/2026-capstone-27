package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.BriefDelivery;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID deliveryId,
        UUID lawyerId,
        String lawyerName,
        String status,
        LocalDateTime sentAt,
        LocalDateTime viewedAt,
        LocalDateTime respondedAt
) {
    public static DeliveryResponse of(BriefDelivery delivery, String lawyerName) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getLawyerId(),
                lawyerName,
                delivery.getStatus().name(),
                delivery.getSentAt(),
                delivery.getViewedAt(),
                delivery.getRespondedAt()
        );
    }
}
