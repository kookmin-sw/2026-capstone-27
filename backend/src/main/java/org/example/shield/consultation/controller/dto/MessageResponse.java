package org.example.shield.consultation.controller.dto;

import org.example.shield.consultation.domain.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        String role,
        String content,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
