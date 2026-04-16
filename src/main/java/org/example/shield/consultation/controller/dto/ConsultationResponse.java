package org.example.shield.consultation.controller.dto;

import org.example.shield.consultation.domain.Consultation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConsultationResponse(
        UUID consultationId,
        String status,
        List<String> primaryField,
        List<String> tags,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        BriefSummary brief
) {
    public record BriefSummary(UUID briefId, String title, String status) {}

    public static ConsultationResponse from(Consultation consultation) {
        // TODO: Brief 연관관계 추가 후 brief 필드 매핑 구현
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getStatus().name(),
                consultation.getPrimaryField(),
                consultation.getTags(),
                consultation.getLastMessage(),
                consultation.getLastMessageAt(),
                consultation.getCreatedAt(),
                null
        );
    }
}
