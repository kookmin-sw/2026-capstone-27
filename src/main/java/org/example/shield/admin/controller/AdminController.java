package org.example.shield.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.shield.admin.application.AdminService;
import org.example.shield.admin.controller.dto.PendingLawyerResponse;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "변호사 가입 심사 목록", description = "검색, 상태 필터, 페이징을 지원하는 변호사 심사 목록을 조회합니다")
    @GetMapping("/lawyers/pending")
    public ApiResponse<PageResponse<PendingLawyerResponse>> getPendingLawyers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<PendingLawyerResponse> result = adminService.getPendingLawyers(keyword, status, pageable);
        return ApiResponse.success("조회 성공", result);
    }

    // TODO: GET  /api/admin/dashboard/stats                        — 대시보드 통계
    // TODO: GET  /api/admin/dashboard/alerts                       — 긴급 알림
    // TODO: PATCH /api/admin/lawyers/{lawyerId}/verification       — 승인/보완요청/거절
    // TODO: GET  /api/admin/lawyers/{lawyerId}/verification-checks — 자동 검증 결과
    // TODO: GET  /api/admin/lawyers/{lawyerId}/documents           — 서류 조회
    // TODO: GET  /api/admin/verification-logs                      — 처리 이력
    // TODO: GET  /api/admin/consultations                          — 상담 모니터링 (차후)
}
