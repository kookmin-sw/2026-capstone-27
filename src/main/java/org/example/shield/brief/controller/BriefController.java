package org.example.shield.brief.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.example.shield.brief.application.BriefService;
import org.example.shield.brief.application.DeliveryService;
import org.example.shield.brief.application.LawyerMatchingService;
import org.example.shield.brief.controller.dto.BriefResponse;
import org.example.shield.brief.controller.dto.BriefSummaryResponse;
import org.example.shield.brief.controller.dto.BriefUpdateRequest;
import org.example.shield.brief.controller.dto.BriefUpdateResponse;
import org.example.shield.brief.controller.dto.DeliveryListResponse;
import org.example.shield.brief.controller.dto.DeliveryRequest;
import org.example.shield.brief.controller.dto.DeliveryResponse;
import org.example.shield.brief.controller.dto.MatchingResponse;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
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

import java.util.UUID;

@Tag(name = "Brief", description = "의뢰서 API")
@RestController
@RequestMapping("/api/briefs")
@RequiredArgsConstructor
public class BriefController {

    private final BriefService briefService;
    private final DeliveryService deliveryService;
    private final LawyerMatchingService lawyerMatchingService;

    @Operation(summary = "내 의뢰서 목록", description = "로그인한 사용자의 의뢰서 목록을 조회합니다")
    @GetMapping
    public ApiResponse<PageResponse<BriefSummaryResponse>> getMyBriefs(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<BriefSummaryResponse> result = briefService.getMyBriefs(userId, pageable);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "의뢰서 상세 조회", description = "의뢰서의 상세 내용을 조회합니다")
    @GetMapping("/{briefId}")
    public ApiResponse<BriefResponse> getBrief(
            @PathVariable UUID briefId,
            @AuthenticationPrincipal UUID userId) {
        BriefResponse result = briefService.getBrief(briefId, userId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "의뢰서 수정", description = "의뢰서를 수정합니다 (내용, 상태, 개인정보 설정)")
    @PatchMapping("/{briefId}")
    public ApiResponse<BriefUpdateResponse> updateBrief(
            @PathVariable UUID briefId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody BriefUpdateRequest request) {
        BriefUpdateResponse result = briefService.updateBrief(briefId, userId, request);
        return ApiResponse.success("의뢰서가 수정되었습니다", result);
    }

    @Operation(summary = "의뢰서 전달", description = "확정된 의뢰서를 변호사에게 전달합니다")
    @PostMapping("/{briefId}/deliveries")
    public ApiResponse<DeliveryResponse> createDelivery(
            @PathVariable UUID briefId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse result = deliveryService.createDelivery(briefId, request.lawyerId(), userId);
        return ApiResponse.success("의뢰서가 전달되었습니다", result);
    }

    @Operation(summary = "전달 현황 조회", description = "의뢰서의 전달 현황을 조회합니다")
    @GetMapping("/{briefId}/deliveries")
    public ApiResponse<DeliveryListResponse> getDeliveries(
            @PathVariable UUID briefId,
            @AuthenticationPrincipal UUID userId) {
        DeliveryListResponse result = deliveryService.getDeliveries(briefId, userId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "변호사 매칭 조회", description = "의뢰서 키워드 기반으로 매칭된 변호사 목록을 조회합니다")
    @GetMapping("/{briefId}/lawyer-recommendations")
    public ApiResponse<PageResponse<MatchingResponse>> getLawyerRecommendations(
            @PathVariable UUID briefId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "experienceYears"));
        PageResponse<MatchingResponse> result = lawyerMatchingService.findMatching(briefId, userId, pageable);
        return ApiResponse.success("매칭 완료", result);
    }
}
