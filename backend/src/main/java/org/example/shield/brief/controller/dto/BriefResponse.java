package org.example.shield.brief.controller.dto;

import org.example.shield.brief.domain.Brief;
import org.example.shield.brief.domain.BriefDelivery;
import org.example.shield.brief.domain.KeyIssue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BriefResponse(
        UUID briefId,
        String title,
        String legalField,
        String content,
        List<KeyIssue> keyIssues,
        List<String> keywords,
        String strategy,
        String privacySetting,
        String status,
        LocalDateTime createdAt,
        UUID acceptedLawyerId,
        String acceptedLawyerName,
        LocalDateTime acceptedAt
) {
    public static BriefResponse from(Brief brief) {
        return of(brief, null, null);
    }

    /**
     * acceptedDelivery 와 lawyerName 이 null 이면 수락된 변호사 없음.
     */
    public static BriefResponse of(Brief brief, BriefDelivery acceptedDelivery, String lawyerName) {
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
                brief.getCreatedAt(),
                acceptedDelivery != null ? acceptedDelivery.getLawyerId() : null,
                lawyerName,
                acceptedDelivery != null ? acceptedDelivery.getRespondedAt() : null
        );
    }
}
