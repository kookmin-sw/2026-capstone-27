package org.example.shield.fcm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.fcm.application.FcmTokenService;
import org.example.shield.fcm.controller.dto.RegisterFcmTokenRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "FCM", description = "푸시 알림 토큰 API")
@RestController
@RequestMapping("/api/fcm/tokens")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 토큰 등록", description = "디바이스의 FCM 토큰을 등록합니다. 동일 토큰이 이미 있으면 사용자/디바이스 정보를 갱신합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RegisterFcmTokenRequest request) {
        fcmTokenService.register(userId, request.token(), request.deviceType());
        return ResponseEntity.ok(ApiResponse.success("FCM 토큰 등록 성공"));
    }

    @Operation(summary = "FCM 토큰 해제", description = "로그아웃 또는 디바이스 비활성화 시 토큰을 제거합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unregister(@RequestParam String token) {
        fcmTokenService.unregister(token);
        return ResponseEntity.ok(ApiResponse.success("FCM 토큰 해제 성공"));
    }
}
