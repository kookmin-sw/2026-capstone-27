package org.example.shield.brief.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.example.shield.common.enums.DeliveryStatus;

public record DeliveryStatusRequest(
        @NotNull(message = "상태는 필수입니다")
        DeliveryStatus status,
        String rejectionReason
) {}
