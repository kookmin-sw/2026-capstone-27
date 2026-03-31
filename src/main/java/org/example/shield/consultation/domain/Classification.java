package org.example.shield.consultation.domain;

/**
 * 분류 결과 엔티티 - classifications 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - consultationId: UUID (FK → consultations.id, UNIQUE)
 * - primaryField: String (CIVIL / CRIMINAL / LABOR / SCHOOL_VIOLENCE)
 * - relatedFields: List<String> (JSONB, 관련 분야 0~2개)
 * - confidence: String (HIGH / MEDIUM / LOW)
 * - reasoning: String (JSONB, 분류 근거)
 * - modifiedBy: String (AI / USER)
 * - confirmedAt: LocalDateTime
 * - createdAt: LocalDateTime
 */
public class Classification {
}
