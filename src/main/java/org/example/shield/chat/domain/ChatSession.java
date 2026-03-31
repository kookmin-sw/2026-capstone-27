package org.example.shield.chat.domain;

/**
 * 채팅 세션 MongoDB Document.
 *
 * TODO: @Document("chat_sessions") 구현
 * - _id: String (MongoDB ObjectId)
 * - consultationId: String (PostgreSQL consultations.id 참조)
 * - userId: String
 * - messages: List<Message>
 *   - Message: { sender: "USER"/"AI", content: String, timestamp: LocalDateTime }
 * - status: String ("active" / "completed")
 * - createdAt, updatedAt: LocalDateTime
 */
public class ChatSession {
}
