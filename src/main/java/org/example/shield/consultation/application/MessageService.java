package org.example.shield.consultation.application;

/**
 * 메시지 서비스 - 챗봇 상담 메시지 전송/조회.
 *
 * Layer: application
 * Called by: ConsultationController.sendMessage(), getMessages()
 * Calls: MessageReader, MessageWriter, ConsultationReader, ConsultationWriter, AiClient
 *
 * TODO:
 * - sendMessage(consultationId, content):
 *   1. messages 테이블에 USER 메시지 INSERT
 *   2. messages 테이블에서 전체 chatHistory 조립
 *   3. AiClient에 { domain, chatHistory } 전송
 *   4. AI Response: { nextQuestion, primaryField, tags, allCompleted }
 *   5. primaryField != null → consultations.primary_field 덮어쓰기
 *   6. tags != null → consultations.tags 덮어쓰기
 *   7. messages 테이블에 AI 메시지 INSERT
 *   8. allCompleted = true → analyze 자동 시작
 *   9. Response: { content, timestamp, allCompleted, classification(완료 시만) }
 *
 * - getMessages(consultationId, pageable):
 *   → messages 테이블에서 sequence 순서로 조회
 */
public class MessageService {
}
