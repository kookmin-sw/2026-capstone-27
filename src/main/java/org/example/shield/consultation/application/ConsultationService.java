package org.example.shield.consultation.application;

/**
 * 상담 서비스 - 상담 생성/조회/목록.
 *
 * Layer: application
 * Called by: ConsultationController
 * Calls: ConsultationRepository, ChatSessionRepository (MongoDB)
 *
 * TODO:
 * - createConsultation(userId):
 *   1. consultations 테이블에 새 row 생성 (status: IN_PROGRESS)
 *   2. MongoDB chat_sessions에 새 세션 생성
 *   3. consultations.chatSessionId에 MongoDB _id 저장
 *
 * - getConsultation(consultationId): 상담 상세 (상태 + briefId 포함)
 * - getMyConsultations(userId, pageable): 내 상담 목록
 */
public class ConsultationService {
}
