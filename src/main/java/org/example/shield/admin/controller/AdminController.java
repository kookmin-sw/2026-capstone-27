package org.example.shield.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.shield.admin.application.AdminService;
import jakarta.validation.Valid;
import org.example.shield.admin.controller.dto.LawyerDetailResponse;
import org.example.shield.admin.controller.dto.PendingLawyerResponse;
import org.example.shield.admin.controller.dto.VerificationChecksResponse;
import org.example.shield.admin.controller.dto.VerificationRequest;
import org.example.shield.admin.controller.dto.VerificationResponse;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.application.LawyerDocumentService;
import org.example.shield.lawyer.controller.dto.DocumentResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final LawyerDocumentService lawyerDocumentService;

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

    @Operation(summary = "변호사 가입 신청 상세", description = "변호사의 상세 프로필 정보를 조회합니다 (관리자 전용)")
    @GetMapping("/lawyers/{lawyerId}")
    public ApiResponse<LawyerDetailResponse> getLawyerDetail(@PathVariable UUID lawyerId) {
        LawyerDetailResponse result = adminService.getLawyerDetail(lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "변호사 인증 심사 처리", description = "변호사 인증 상태를 변경합니다 (승인/검토 중/보완요청/거절)")
    @PatchMapping("/lawyers/{lawyerId}/verification")
    public ApiResponse<VerificationResponse> processVerification(
            @PathVariable UUID lawyerId,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody VerificationRequest request) {
        VerificationResponse result = adminService.processVerification(
                lawyerId, adminId, request.status(), request.reason());
        return ApiResponse.success("처리 완료", result);
    }

    @Operation(summary = "자동 검증 결과 조회", description = "변호사 인증 신청의 자동 검증 결과와 체크리스트를 조회합니다")
    @GetMapping("/lawyers/{lawyerId}/verification-checks")
    public ApiResponse<VerificationChecksResponse> getVerificationChecks(@PathVariable UUID lawyerId) {
        VerificationChecksResponse result = adminService.getVerificationChecks(lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "변호사 서류 조회", description = "변호사가 업로드한 서류 목록을 조회합니다")
    @GetMapping("/lawyers/{lawyerId}/documents")
    public ApiResponse<List<DocumentResponse>> getLawyerDocuments(@PathVariable UUID lawyerId) {
        List<DocumentResponse> result = lawyerDocumentService.getDocuments(lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    // TODO: GET  /api/admin/dashboard/stats                        — 대시보드 통계
    // TODO: GET  /api/admin/dashboard/alerts                       — 긴급 알림
    // TODO: GET  /api/admin/verification-logs                      — 처리 이력
    // TODO: GET  /api/admin/consultations                          — 상담 모니터링 (차후)
}
