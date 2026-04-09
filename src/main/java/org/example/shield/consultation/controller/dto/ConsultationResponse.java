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
        LocalDateTime createdAt
) {
    public static ConsultationResponse from(Consultation consultation) {
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getStatus().name(),
                consultation.getPrimaryField(),
                consultation.getTags(),
                consultation.getLastMessage(),
                consultation.getLastMessageAt(),
                consultation.getCreatedAt()
        );
    }
}
