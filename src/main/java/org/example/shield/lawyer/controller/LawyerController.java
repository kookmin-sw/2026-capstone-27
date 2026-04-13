package org.example.shield.lawyer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.application.LawyerDocumentService;
import org.example.shield.lawyer.application.LawyerService;
import org.example.shield.lawyer.application.VerificationService;
import org.example.shield.lawyer.controller.dto.DocumentResponse;
import org.example.shield.lawyer.controller.dto.LawyerResponse;
import org.example.shield.lawyer.controller.dto.ProfileUpdateRequest;
import org.example.shield.lawyer.controller.dto.VerificationRequest;
import org.example.shield.lawyer.controller.dto.VerificationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Lawyer", description = "변호사 API")
@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
public class LawyerController {

    private final LawyerService lawyerService;
    private final VerificationService verificationService;
    private final LawyerDocumentService lawyerDocumentService;

    @Operation(summary = "변호사 목록 조회", description = "인증된 변호사 목록을 경력순으로 조회합니다")
    @GetMapping
    public ApiResponse<PageResponse<LawyerResponse>> getLawyers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "experienceYears"));
        PageResponse<LawyerResponse> result = lawyerService.getLawyers(pageable);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "변호사 프로필 상세", description = "변호사 프로필을 상세 조회합니다 (의뢰인용)")
    @GetMapping("/{lawyerId}")
    public ApiResponse<LawyerResponse> getLawyer(@PathVariable UUID lawyerId) {
        LawyerResponse result = lawyerService.getLawyer(lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "내 프로필 조회", description = "변호사 본인의 프로필을 조회합니다")
    @GetMapping("/me")
    public ApiResponse<LawyerResponse> getMyProfile(
            @AuthenticationPrincipal UUID userId) {
        LawyerResponse result = lawyerService.getMyProfile(userId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "내 프로필 수정", description = "변호사 본인의 프로필을 수정합니다")
    @PatchMapping("/me")
    public ApiResponse<LawyerResponse> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @RequestBody ProfileUpdateRequest request) {
        LawyerResponse result = lawyerService.updateMyProfile(userId, request);
        return ApiResponse.success("프로필이 수정되었습니다", result);
    }

    @Operation(summary = "검증 상태 확인", description = "변호사 자격 검증 상태를 확인합니다")
    @GetMapping("/me/verification-status")
    public ApiResponse<VerificationResponse> getVerificationStatus(
            @AuthenticationPrincipal UUID userId) {
        VerificationResponse result = verificationService.getVerificationStatus(userId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "검증 신청", description = "대한변호사협회 등록번호로 자격 검증을 신청합니다")
    @PostMapping("/me/verification-request")
    public ApiResponse<VerificationResponse> requestVerification(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VerificationRequest request) {
        VerificationResponse result = verificationService.requestVerification(
                userId, request.barAssociationNumber());
        return ApiResponse.success("검증 신청이 완료되었습니다", result);
    }

    @Operation(summary = "서류 업로드", description = "변호사 자격증 등 서류를 업로드합니다 (PDF, JPG, PNG / 최대 10MB)")
    @PostMapping(value = "/me/documents", consumes = "multipart/form-data")
    public ApiResponse<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) {
        DocumentResponse result = lawyerDocumentService.uploadDocument(userId, file);
        return ApiResponse.success("서류가 업로드되었습니다", result);
    }
}
