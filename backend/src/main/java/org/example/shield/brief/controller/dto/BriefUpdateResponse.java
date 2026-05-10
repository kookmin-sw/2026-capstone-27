package org.example.shield.brief.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BriefUpdateResponse(
        UUID briefId,
        String status,
        LocalDateTime updatedAt
) {}
