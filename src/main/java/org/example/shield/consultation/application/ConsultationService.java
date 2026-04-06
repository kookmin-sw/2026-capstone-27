package org.example.shield.consultation.application;

/**
 * 상담 서비스 - 상담 생성/조회/목록.
 *
 * Layer: application
 * Called by: ConsultationController
 * Calls: ConsultationReader, ConsultationWriter
 *
 * TODO:
 * - createConsultation(userId):
 *   1. consultations 테이블에 새 row 생성 (status: CLASSIFYING)
 *   2. chat_messages JSONB 빈 배열로 초기화
 *
 * - getMyConsultations(userId, pageable): 내 상담 목록 (brief 포함)
 */
public class ConsultationService {
}
