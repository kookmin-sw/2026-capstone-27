package org.example.shield.brief.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryRequest(
        @NotNull(message = "변호사 ID는 필수입니다")
        UUID lawyerId
) {}
