package org.example.shield.consultation.controller.dto;

import org.example.shield.consultation.domain.Consultation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConsultationResponse(
        UUID consultationId,
        String status,
        List<String> userDomains,
        List<String> userSubDomains,
        List<String> userTags,
        List<String> aiDomains,
        List<String> aiSubDomains,
        List<String> aiTags,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        BriefSummary brief
) {
    public record BriefSummary(UUID briefId, String title, String status) {}

    public static ConsultationResponse from(Consultation consultation, BriefSummary brief) {
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getStatus().name(),
                consultation.getUserDomains(),
                consultation.getUserSubDomains(),
                consultation.getUserTags(),
                consultation.getAiDomains(),
                consultation.getAiSubDomains(),
                consultation.getAiTags(),
                consultation.getLastMessage(),
                consultation.getLastMessageAt(),
                consultation.getCreatedAt(),
                brief
        );
    }
}
