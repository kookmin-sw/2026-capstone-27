package org.example.shield.consultation.domain;

/**
 * 상담 엔티티 - consultations 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - userId: UUID (FK -> users.id)
 * - status: ConsultationStatus (COLLECTING / ANALYZING / AWAITING_CONFIRM / CONFIRMED / REJECTED)
 * - selectedDomain: DomainType (nullable, "잘 모르겠어요" = null)
 * - primaryField: JSONB (nullable, AI 분류 결과, 복수 가능)
 * - tags: JSONB (nullable, AI 분류 태그)
 * - createdAt, updatedAt: LocalDateTime
 *
 * 대화 내역은 messages 테이블에 별도 저장 (1:N 관계)
 */
public class Consultation {
}
