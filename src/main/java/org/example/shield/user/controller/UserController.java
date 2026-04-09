package org.example.shield.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.user.application.UserService;
import org.example.shield.user.controller.dto.UserResponse;
import org.example.shield.user.controller.dto.UserUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal UUID userId) {
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateMyInfo(userId, request);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 수정 성공", response));
    }
}
