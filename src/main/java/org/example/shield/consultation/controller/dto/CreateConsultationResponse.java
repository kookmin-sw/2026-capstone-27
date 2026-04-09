package org.example.shield.consultation.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateConsultationResponse(
        UUID consultationId,
        String status,
        String welcomeMessage,
        LocalDateTime createdAt
) {}
