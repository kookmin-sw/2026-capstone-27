package org.example.shield.admin.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationResponse(
        UUID lawyerId,
        String previousStatus,
        String newStatus,
        String reason,
        LocalDateTime processedAt
) {}
