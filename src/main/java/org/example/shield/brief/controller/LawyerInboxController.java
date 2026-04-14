package org.example.shield.brief.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.brief.application.DeliveryService;
import org.example.shield.brief.controller.dto.DeliveryStatusRequest;
import org.example.shield.brief.controller.dto.DeliveryStatusResponse;
import org.example.shield.brief.controller.dto.InboxDetailResponse;
import org.example.shield.brief.controller.dto.InboxResponse;
import org.example.shield.brief.controller.dto.InboxStatsResponse;
import org.example.shield.common.enums.DeliveryStatus;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Lawyer Inbox", description = "변호사 수신함 API")
@RestController
@RequestMapping("/api/lawyer/inbox")
@RequiredArgsConstructor
public class LawyerInboxController {

    private final DeliveryService deliveryService;

    @Operation(summary = "수신 의뢰서 목록", description = "변호사에게 전달된 의뢰서 목록을 조회합니다. status 파라미터로 필터링 가능")
    @GetMapping
    public ApiResponse<PageResponse<InboxResponse>> getInbox(
            @AuthenticationPrincipal UUID lawyerId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        PageResponse<InboxResponse> result = deliveryService.getInbox(lawyerId, status, pageable);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "수신함 통계", description = "변호사 수신함의 상태별 통계를 조회합니다")
    @GetMapping("/stats")
    public ApiResponse<InboxStatsResponse> getInboxStats(
            @AuthenticationPrincipal UUID lawyerId) {
        InboxStatsResponse result = deliveryService.getInboxStats(lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "수신 의뢰서 상세", description = "전달받은 의뢰서의 상세 내용을 조회합니다")
    @GetMapping("/{deliveryId}")
    public ApiResponse<InboxDetailResponse> getInboxDetail(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal UUID lawyerId) {
        InboxDetailResponse result = deliveryService.getInboxDetail(deliveryId, lawyerId);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "수신 의뢰서 수락/거절", description = "전달받은 의뢰서를 수락하거나 거절합니다")
    @PreAuthorize("hasRole('LAWYER')")
    @PatchMapping("/{deliveryId}/status")
    public ApiResponse<DeliveryStatusResponse> updateDeliveryStatus(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal UUID lawyerId,
            @Valid @RequestBody DeliveryStatusRequest request) {
        DeliveryStatusResponse result = deliveryService.updateDeliveryStatus(
                deliveryId, lawyerId, request.status(), request.rejectionReason());
        return ApiResponse.success("처리 완료", result);
    }
}
