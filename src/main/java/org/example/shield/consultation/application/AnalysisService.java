package org.example.shield.consultation.application;

/**
 * 분석 서비스 - 도메인 에이전트로 의뢰서 생성 (비동기).
 *
 * Layer: application
 * Called by: ConsultationController.analyze()
 * Calls: ConsultationReader, BriefWriter, AiClient, NotificationSender
 *
 * TODO:
 * - analyze(consultationId): @Async 비동기 처리
 *   1. consultations.chat_messages JSONB에서 대화 내역 조회
 *   2. 누락 정보 검증
 *   3. AiClient.generateBrief(대화 내역) → 도메인 에이전트(Grok)로 의뢰서 생성
 *   4. briefs 테이블에 저장 (legal_field, keywords, content, status: DRAFT)
 *   5. consultations.status → COMPLETED
 *   6. NotificationSender로 의뢰인에게 이메일 알림
 */
public class AnalysisService {
}
