package org.example.shield.lawyer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.lawyer.application.LawyerDocumentService;
import org.example.shield.lawyer.controller.dto.DocumentResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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

    private final LawyerDocumentService lawyerDocumentService;

    @Operation(summary = "서류 업로드", description = "변호사 자격증 등 서류를 업로드합니다 (PDF, JPG, PNG / 최대 10MB)")
    @PostMapping("/me/documents")
    public ApiResponse<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) {
        DocumentResponse result = lawyerDocumentService.uploadDocument(userId, file);
        return ApiResponse.success("서류가 업로드되었습니다", result);
    }

    // TODO: GET   /api/lawyers                             — 변호사 목록 조회
    // TODO: GET   /api/lawyers/{lawyerId}                  — 변호사 프로필 상세 (의뢰인용)
    // TODO: GET   /api/lawyers/me                          — 내 프로필 조회 (차후 구현)
    // TODO: PATCH /api/lawyers/me                          — 내 프로필 수정 (차후 구현)
    // TODO: POST  /api/lawyers/me/verification-request     — 검증 신청
    // TODO: GET   /api/lawyers/me/verification-status      — 검증 상태 확인
}
