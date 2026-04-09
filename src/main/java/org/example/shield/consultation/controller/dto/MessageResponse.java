package org.example.shield.consultation.controller.dto;

import org.example.shield.consultation.domain.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        String sender,
        String content,
        LocalDateTime timestamp
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
