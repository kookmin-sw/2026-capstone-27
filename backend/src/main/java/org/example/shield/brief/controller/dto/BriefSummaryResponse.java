package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;

import java.time.LocalDateTime;
import java.util.UUID;

public record BriefSummaryResponse(
        UUID briefId,
        String title,
        String status,
        LocalDateTime createdAt,
        UUID acceptedLawyerId,
        String acceptedLawyerName,
        LocalDateTime acceptedAt
) {
    public static BriefSummaryResponse from(Brief brief) {
        return of(brief, null, null);
    }

    public static BriefSummaryResponse of(Brief brief, BriefDelivery acceptedDelivery, String lawyerName) {
        return new BriefSummaryResponse(
                brief.getId(),
                brief.getTitle(),
                brief.getStatus().name(),
                brief.getCreatedAt(),
                acceptedDelivery != null ? acceptedDelivery.getLawyerId() : null,
                lawyerName,
                acceptedDelivery != null ? acceptedDelivery.getRespondedAt() : null
        );
    }
}
