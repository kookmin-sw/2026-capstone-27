package org.example.shield.consultation.domain;

/**
 * 메시지 엔티티 - messages 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - consultationId: UUID (FK -> consultations.id)
 * - sequence: Integer (AUTO INCREMENT, 메시지 순서)
 * - role: MessageRole ENUM (USER / AI)
 * - content: String (메시지 내용)
 * - model: String (nullable, AI 모델명: grok-3 등)
 * - tokensInput: Integer (nullable, 입력 토큰 수)
 * - tokensOutput: Integer (nullable, 출력 토큰 수)
 * - latencyMs: Integer (nullable, AI 응답 시간 ms)
 * - parentMessageId: UUID (nullable, 이전 메시지 참조)
 * - createdAt: LocalDateTime
 */
public class Message {
}
