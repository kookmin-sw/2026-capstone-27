package org.example.shield.admin.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationLogResponse(
        UUID logId,
        String lawyerName,
        String fromStatus,
        String toStatus,
        String specializations,
        String adminName,
        String reason,
        LocalDateTime createdAt
) {}
