package org.example.shield.consultation.domain;

/**
 * 상담 엔티티 - consultations 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - userId: UUID (FK → users.id)
 * - status: String (IN_PROGRESS / ANALYZED / COMPLETED)
 * - classificationStatus: String (NONE / CLASSIFIED / CONFIRMED)
 * - structureStatus: String (NONE / DRAFT / CONFIRMED)
 * - chatSessionId: String (MongoDB chat_sessions._id 참조)
 * - createdAt, updatedAt: LocalDateTime
 */
public class Consultation {
}
