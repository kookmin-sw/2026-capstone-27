package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InboxDetailResponse(
        UUID deliveryId,
        UUID briefId,
        String title,
        String legalField,
        String content,
        List<String> keywords,
        String status,
        String clientName,
        String clientEmail,
        LocalDateTime sentAt
) {
    public static InboxDetailResponse of(BriefDelivery delivery, Brief brief,
                                          String clientName, String clientEmail) {
        return new InboxDetailResponse(
                delivery.getId(),
                brief.getId(),
                brief.getTitle(),
                brief.getLegalField(),
                brief.getContent(),
                brief.getKeywords(),
                delivery.getStatus().name(),
                clientName,
                clientEmail,
                delivery.getSentAt()
        );
    }
}
