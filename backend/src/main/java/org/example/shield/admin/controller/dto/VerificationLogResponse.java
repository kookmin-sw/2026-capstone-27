package org.example.shield.admin.controller.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VerificationLogResponse(
        UUID logId,
        String lawyerName,
        String fromStatus,
        String toStatus,
        List<String> domains,
        String adminName,
        String reason,
        LocalDateTime createdAt
) {}
