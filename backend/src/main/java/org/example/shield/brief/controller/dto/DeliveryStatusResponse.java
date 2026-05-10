package org.example.shield.brief.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryStatusResponse(
        UUID deliveryId,
        String status,
        LocalDateTime respondedAt
) {}
