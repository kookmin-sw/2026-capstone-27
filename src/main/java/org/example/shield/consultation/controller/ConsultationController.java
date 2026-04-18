package org.example.shield.consultation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shield.common.enums.ConsultationStatus;
import org.example.shield.common.response.ApiResponse;
import org.example.shield.common.response.PageResponse;
import org.example.shield.consultation.application.AnalysisService;
import org.example.shield.consultation.application.ConsultationService;
import org.example.shield.consultation.application.MessageService;
import org.example.shield.consultation.controller.dto.ConsultationResponse;
import org.example.shield.consultation.controller.dto.ClassifyRequest;
import org.example.shield.consultation.controller.dto.ClassifyResponse;
import org.example.shield.consultation.controller.dto.CreateConsultationRequest;
import org.example.shield.consultation.controller.dto.CreateConsultationResponse;
import org.example.shield.consultation.controller.dto.MessageRequest;
import org.example.shield.consultation.controller.dto.MessageResponse;
import org.example.shield.consultation.controller.dto.SendMessageResponse;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Consultation", description = "상담 API")
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final MessageService messageService;
    private final AnalysisService analysisService;
    private final ConsultationReader consultationReader;
    private final ConsultationWriter consultationWriter;

    @Operation(summary = "상담 생성", description = "새로운 상담을 생성하고 환영 메시지를 반환합니다")
    @PostMapping
    public ApiResponse<CreateConsultationResponse> create(
            @RequestBody CreateConsultationRequest request,
            @AuthenticationPrincipal UUID userId) {
        CreateConsultationResponse result = consultationService.createConsultation(
                userId, request.domains(), request.subDomains(), request.tags());
        return ApiResponse.success("상담이 생성되었습니다", result);
    }

    @Operation(summary = "상담 상세 조회", description = "상담의 상태, 분류, 태그 등 메타정보를 조회합니다")
    @GetMapping("/{consultationId}")
    public ApiResponse<ConsultationResponse> getConsultation(
            @PathVariable UUID consultationId) {
        ConsultationResponse response = consultationService.getConsultation(consultationId);
        return ApiResponse.success("조회 성공", response);
    }

    @Operation(summary = "내 상담 목록", description = "로그인한 사용자의 상담 목록을 조회합니다")
    @GetMapping
    public ApiResponse<PageResponse<ConsultationResponse>> getMyConsultations(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
        PageResponse<ConsultationResponse> result = consultationService.getMyConsultations(userId, pageable);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "메시지 전송", description = "상담에 메시지를 전송합니다")
    @PostMapping("/{consultationId}/messages")
    public ResponseEntity<ApiResponse<SendMessageResponse>> sendMessage(
            @PathVariable UUID consultationId,
            @Valid @RequestBody MessageRequest request) {
        SendMessageResponse result = messageService.sendMessage(consultationId, request.content());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("전송 완료", result));
    }

    @Operation(summary = "메시지 목록 조회", description = "상담의 대화 내역을 조회합니다")
    @GetMapping("/{consultationId}/messages")
    public ApiResponse<PageResponse<MessageResponse>> getMessages(
            @PathVariable UUID consultationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "sequence"));
        PageResponse<MessageResponse> result = messageService.getMessages(consultationId, pageable);
        return ApiResponse.success("조회 성공", result);
    }

    @Operation(summary = "분류 직접 수정", description = "AI 분류 결과를 사용자가 직접 수정합니다")
    @PatchMapping("/{consultationId}/classify")
    public ApiResponse<ClassifyResponse> updateClassification(
            @PathVariable UUID consultationId,
            @Valid @RequestBody ClassifyRequest request) {
        ClassifyResponse result = consultationService.updateClassification(
                consultationId, request.domains(), request.subDomains(), request.tags());
        return ApiResponse.success("분류가 수정되었습니다", result);
    }

    @Operation(summary = "의뢰서 생성 요청", description = "AI가 대화 내용을 기반으로 의뢰서를 비동기 생성합니다")
    @PostMapping("/{consultationId}/analyze")
    public ResponseEntity<ApiResponse<Void>> analyze(
            @PathVariable UUID consultationId) {

        // P0-IV 멱등성 가드: 원자적 상태 전이 COLLECTING → ANALYZING
        Consultation consultation = consultationReader.findById(consultationId);
        if (consultation.getStatus() != ConsultationStatus.COLLECTING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 분석이 진행 중이거나 완료된 상담입니다"));
        }

        consultation.updateStatus(ConsultationStatus.ANALYZING);
        consultationWriter.save(consultation);

        // 비동기 의뢰서 생성 시작
        analysisService.analyzeAsync(consultationId);

        return ResponseEntity.accepted()
                .body(ApiResponse.success("의뢰서 생성이 시작되었습니다", null));
    }
}
