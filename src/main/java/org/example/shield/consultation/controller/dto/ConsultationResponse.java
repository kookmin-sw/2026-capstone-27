package org.example.shield.consultation.controller.dto;

/**
 * 상담 상세 응답 DTO.
 *
 * TODO:
 * - consultationId: String (UUID)
 * - status: String (IN_PROGRESS / ANALYZED / COMPLETED)
 * - briefId: String (생성된 의뢰서 ID, null이면 분석 전)
 * - classification: Object (primaryField, confidence) (null이면 분석 전)
 * - createdAt: LocalDateTime
 */
public class ConsultationResponse {
}
