package org.example.shield.consultation.domain;

/**
 * 상담 엔티티 - consultations 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - userId: UUID (FK -> users.id)
 * - status: ConsultationStatus (CLASSIFYING / IN_PROGRESS / COMPLETED / REJECTED)
 * - primaryField: String (nullable, 분류 전 null)
 * - chatMessages: JSONB (대화 내역 배열)
 * - createdAt, updatedAt: LocalDateTime
 */
public class Consultation {
}
