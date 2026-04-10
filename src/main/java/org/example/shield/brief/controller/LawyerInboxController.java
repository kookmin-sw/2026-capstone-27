package org.example.shield.brief.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.shield.brief.application.DeliveryService;
import org.example.shield.brief.controller.dto.InboxDetailResponse;
import org.example.shield.brief.controller.dto.InboxResponse;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "수신 의뢰서 목록", description = "변호사에게 전달된 의뢰서 목록을 조회합니다")
    @GetMapping
    public ApiResponse<PageResponse<InboxResponse>> getInbox(
            @AuthenticationPrincipal UUID lawyerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        PageResponse<InboxResponse> result = deliveryService.getInbox(lawyerId, pageable);
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
}
