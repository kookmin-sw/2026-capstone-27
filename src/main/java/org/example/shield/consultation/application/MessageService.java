package org.example.shield.consultation.application;

/**
 * 메시지 서비스 - 챗봇 상담 메시지 전송/조회.
 *
 * Layer: application
 * Called by: ConsultationController.sendMessage(), getMessages()
 * Calls: ConsultationReader, ConsultationWriter, AiClient
 *
 * TODO:
 * - sendMessage(consultationId, content):
 *   1. USER 메시지를 consultations.chat_messages JSONB에 저장
 *   2. AiClient로 다음 질문 생성
 *   3. AI 메시지도 chat_messages에 저장
 *   4. completed이면 AI 마무리 멘트 저장
 *   5. MessageResponse (userMessage + aiMessage + formProgress) 반환
 *
 * - getMessages(consultationId, pageable): chat_messages JSONB에서 메시지 조회
 */
public class MessageService {
}
