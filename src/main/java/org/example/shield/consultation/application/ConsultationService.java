package org.example.shield.consultation.application;

/**
 * 상담 서비스 - 상담 생성/조회/목록.
 *
 * Layer: application
 * Called by: ConsultationController
 * Calls: ConsultationReader, ConsultationWriter, MessageWriter
 *
 * TODO:
 * - createConsultation(userId, domain):
 *   1. consultations 테이블에 새 row 생성 (status: COLLECTING)
 *   2. selected_domain 저장 (nullable)
 *   3. 환영 메시지를 messages 테이블에 AI 메시지로 저장
 *   4. welcomeMessage 반환
 *
 * - getMyConsultations(userId, pageable): 내 상담 목록
 *   → brief, primaryField, tags, lastMessage 포함
 */
public class ConsultationService {
}
