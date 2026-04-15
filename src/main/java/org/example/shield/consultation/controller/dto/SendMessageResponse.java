package org.example.shield.consultation.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.shield.consultation.domain.Message;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendMessageResponse(
        String content,
        LocalDateTime timestamp,
        boolean allCompleted,
        Classification classification
) {
    public static SendMessageResponse from(Message message, boolean allCompleted) {
        return new SendMessageResponse(
                message.getContent(),
                message.getCreatedAt(),
                allCompleted,
                null
        );
    }

    public static SendMessageResponse from(Message message, boolean allCompleted,
                                           List<String> primaryField, List<String> tags) {
        Classification classif = (allCompleted && primaryField != null)
                ? new Classification(primaryField, tags)
                : null;
        return new SendMessageResponse(
                message.getContent(),
                message.getCreatedAt(),
                allCompleted,
                classif
        );
    }

    public record Classification(
            List<String> primaryField,
            List<String> tags
    ) {}
}
