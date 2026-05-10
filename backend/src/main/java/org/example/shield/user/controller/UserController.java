package org.example.shield.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.user.application.UserService;
import org.example.shield.user.controller.dto.UserResponse;
import org.example.shield.user.controller.dto.UserUpdateRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보 조회")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(
            @AuthenticationPrincipal UUID userId) {
        UserResponse response = userService.getMyInfo(userId);
        return ApiResponse.success("조회 성공", response);
    }

    @Operation(summary = "내 정보 수정", description = "이름, 전화번호를 선택적으로 수정합니다")
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(
            @AuthenticationPrincipal UUID userId,
            @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateMyInfo(userId, request);
        return ApiResponse.success("수정 완료", response);
    }
}
