package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.Brief;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BriefResponse(
        UUID briefId,
        String title,
        String legalField,
        String content,
        List<String> keyIssues,
        List<String> keywords,
        String strategy,
        String privacySetting,
        String status,
        LocalDateTime createdAt
) {
    public static BriefResponse from(Brief brief) {
        return new BriefResponse(
                brief.getId(),
                brief.getTitle(),
                brief.getLegalField(),
                brief.getContent(),
                brief.getKeyIssues(),
                brief.getKeywords(),
                brief.getStrategy(),
                brief.getPrivacySetting().name(),
                brief.getStatus().name(),
                brief.getCreatedAt()
        );
    }
}
