package org.example.shield.consultation.application;

/**
 * 분석 서비스 - 의뢰서 생성 (비동기).
 *
 * Layer: application
 * Called by: MessageService (allCompleted 시), ConsultationController.analyze()
 * Calls: MessageReader, ConsultationReader, BriefWriter, ConsultationWriter, AiClient, NotificationSender
 *
 * TODO:
 * - analyze(consultationId): @Async 비동기 처리
 *   1. messages 테이블에서 전체 chatHistory 조립
 *   2. AiClient에 { domain, chatHistory } 전송 (의뢰서 생성용)
 *   3. AI Response: { title, content, keyIssues, keywords, strategy }
 *   4. briefs 테이블에 저장
 *   5. consultations.status → AWAITING_CONFIRM
 *   6. NotificationSender로 의뢰인에게 이메일 알림
 *
 * 실패 시:
 *   → consultations.status → REJECTED
 *   → 사용자에게 실패 알림
 */
public class AnalysisService {
}
