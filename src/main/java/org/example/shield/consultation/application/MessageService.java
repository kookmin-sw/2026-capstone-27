package org.example.shield.consultation.application;

/**
 * 메시지 서비스 - 챗봇 상담 메시지 전송/조회.
 *
 * Layer: application
 * Called by: ConsultationController.sendMessage(), getMessages()
 * Calls: ChatSessionRepository (MongoDB), GrokService, Redis (캐시)
 *
 * TODO:
 * - sendMessage(consultationId, content):
 *   1. 유저 메시지를 MongoDB chat_sessions.messages[]에 저장
 *   2. Redis 캐시 갱신 (선택)
 *   3. GrokService.chatbotResponse(전체 대화)로 AI 응답 생성
 *   4. AI 응답도 MongoDB에 저장
 *   5. MessageResponse (userMessage + aiMessage) 반환
 *
 * - getMessages(consultationId, pageable): MongoDB에서 메시지 이력 조회
 */
public class MessageService {
}
