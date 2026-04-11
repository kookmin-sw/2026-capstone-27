package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.Brief;

import java.time.LocalDateTime;
import java.util.UUID;

public record BriefSummaryResponse(
        UUID briefId,
        String title,
        String status,
        LocalDateTime createdAt
) {
    public static BriefSummaryResponse from(Brief brief) {
        return new BriefSummaryResponse(
                brief.getId(),
                brief.getTitle(),
                brief.getStatus().name(),
                brief.getCreatedAt()
        );
    }
}
