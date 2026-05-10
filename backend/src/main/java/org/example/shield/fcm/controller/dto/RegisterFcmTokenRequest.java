package org.example.shield.fcm.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.shield.fcm.domain.DeviceType;

public record RegisterFcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다")
        String token,

        @NotNull(message = "디바이스 타입은 필수입니다")
        DeviceType deviceType
) {}
