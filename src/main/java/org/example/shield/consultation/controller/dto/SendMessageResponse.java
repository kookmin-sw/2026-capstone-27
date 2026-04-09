package org.example.shield.consultation.controller.dto;

import org.example.shield.consultation.domain.Message;

import java.time.LocalDateTime;

public record SendMessageResponse(
        String content,
        LocalDateTime timestamp,
        boolean allCompleted
) {
    public static SendMessageResponse from(Message message, boolean allCompleted) {
        return new SendMessageResponse(
                message.getContent(),
                message.getCreatedAt(),
                allCompleted
        );
    }
}
