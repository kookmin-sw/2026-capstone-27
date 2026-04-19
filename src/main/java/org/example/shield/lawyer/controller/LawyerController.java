package org.example.shield.lawyer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.example.shield.common.response.PageResponse;
import org.example.shield.lawyer.application.LawyerDocumentService;
import org.example.shield.lawyer.application.LawyerService;
import org.example.shield.lawyer.application.VerificationService;
import org.example.shield.lawyer.controller.dto.DocumentResponse;
import org.example.shield.lawyer.controller.dto.LawyerRegisterRequest;
import org.example.shield.lawyer.controller.dto.LawyerRegisterResponse;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Lawyer", description = "변호사 API")
@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
public class LawyerController {

    private final LawyerService lawyerService;
    private final VerificationService verificationService;
    private final LawyerDocumentService lawyerDocumentService;

    @Operation(summary = "변호사 목록 조회", description = "인증된 변호사 목록을 조회합니다. 전문분야, 최소경력, 정렬 기준으로 필터링 가능")
    @GetMapping
    public ApiResponse<PageResponse<LawyerResponse>> getLawyers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(defaultValue = "experience") String sort) {
        Sort sortOrder = switch (sort) {
            case "name" -> Sort.by(Sort.Direction.ASC, "userId");
            default -> Sort.by(Sort.Direction.DESC, "experience_years");
        };
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sortOrder);
        PageResponse<LawyerResponse> result = lawyerService.getLawyers(pageable, specialization, minExperience);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "변호사 프로필 상세", description = "변호사 프로필을 상세 조회합니다 (의뢰인용)")
    @GetMapping("/{lawyerId}")
    public ApiResponse<LawyerResponse> getLawyer(@PathVariable UUID lawyerId) {
        LawyerResponse result = lawyerService.getLawyer(lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "내 프로필 조회", description = "변호사 본인의 프로필을 조회합니다")
    @PreAuthorize("hasRole('LAWYER')")
    @GetMapping("/me")
    public ApiResponse<LawyerResponse> getMyProfile(
            @AuthenticationPrincipal UUID userId) {
        LawyerResponse result = lawyerService.getMyProfile(userId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "내 프로필 수정", description = "변호사 본인의 프로필을 수정합니다")
    @PreAuthorize("hasRole('LAWYER')")
    @PatchMapping("/me")
    public ApiResponse<LawyerResponse> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        LawyerResponse result = lawyerService.updateMyProfile(userId, request);
        return ApiResponse.success("프로필이 수정되었습니다", result);
    }

    @Operation(summary = "검증 상태 확인", description = "변호사 자격 검증 상태를 확인합니다")
    @PreAuthorize("hasRole('LAWYER')")
    @GetMapping("/me/verification-status")
    public ApiResponse<VerificationResponse> getVerificationStatus(
            @AuthenticationPrincipal UUID userId) {
        VerificationResponse result = verificationService.getVerificationStatus(userId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(
            summary = "변호사 가입",
            description = "소셜 로그인 이후 변호사 추가정보 입력 + 검증 신청을 통합 처리합니다. "
                    + "LawyerProfile을 생성하고 PENDING 상태로 설정하며, User.role을 USER→LAWYER로 승격한 뒤 "
                    + "새 accessToken을 body에, 새 refreshToken을 HttpOnly 쿠키로 내려줍니다. "
                    + "이미 LawyerProfile이 존재하면 VERIFICATION_ALREADY_SUBMITTED 에러를 반환합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/register")
    public ApiResponse<LawyerRegisterResponse> register(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody LawyerRegisterRequest request,
            HttpServletResponse response) {
        VerificationService.RegisterResult result = verificationService.register(userId, request);
        addRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.success("변호사 가입이 완료되었습니다", result.response());
    }

    /**
     * 로그인 엔드포인트(/api/auth/google 등)과 동일한 속성으로 refreshToken 쿠키를 교체한다.
     * AuthController 와 중복되는 로직이지만 컨트롤러 경계를 넘어 공유하기는 부적절하여 동일 설정을 재사용한다.
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    @Operation(summary = "검증 신청", description = "대한변호사협회 등록번호로 자격 검증을 신청합니다")
    @PreAuthorize("hasRole('LAWYER')")
    @PostMapping("/me/verification-request")
    public ApiResponse<VerificationResponse> requestVerification(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VerificationRequest request) {
        VerificationResponse result = verificationService.requestVerification(
                userId, request.barAssociationNumber());
        return ApiResponse.success("검증 신청이 완료되었습니다", result);
    }

    @Operation(summary = "서류 업로드", description = "변호사 자격증 등 서류를 업로드합니다 (PDF, JPG, PNG / 최대 10MB)")
    @PreAuthorize("hasRole('LAWYER')")
    @PostMapping(value = "/me/documents", consumes = "multipart/form-data")
    public ApiResponse<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) {
        DocumentResponse result = lawyerDocumentService.uploadDocument(userId, file);
        return ApiResponse.success("서류가 업로드되었습니다", result);
    }

    @Operation(summary = "내 서류 조회", description = "변호사 본인이 업로드한 서류 목록을 조회합니다")
    @PreAuthorize("hasRole('LAWYER')")
    @GetMapping("/me/documents")
    public ApiResponse<List<DocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal UUID userId) {
        List<DocumentResponse> result = lawyerDocumentService.getMyDocuments(userId);
        return ApiResponse.success("조회 성공", result);
    }
}
