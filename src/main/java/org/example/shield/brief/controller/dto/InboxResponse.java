package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;

import java.time.LocalDateTime;
import java.util.UUID;

public record InboxResponse(
        UUID deliveryId,
        String briefTitle,
        String legalField,
        String status,
        LocalDateTime sentAt
) {
    public static InboxResponse of(BriefDelivery delivery, Brief brief) {
        return new InboxResponse(
                delivery.getId(),
                brief.getTitle(),
                brief.getLegalField(),
                delivery.getStatus().name(),
                delivery.getSentAt()
        );
    }
}
