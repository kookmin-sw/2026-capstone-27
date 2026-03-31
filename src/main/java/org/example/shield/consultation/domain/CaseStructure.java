package org.example.shield.consultation.domain;

/**
 * 사건 구조화 결과 엔티티 - case_structures 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - consultationId: UUID (FK → consultations.id, UNIQUE)
 * - overview: String (TEXT, 사건 개요)
 * - parties: String (TEXT, 당사자 관계)
 * - timeline: String (TEXT, 사건 경위)
 * - desiredOutcome: String (TEXT, 의뢰인이 원하는 결과)
 * - keyIssues: String (JSONB, [{title, description}])
 * - keywords: String (JSONB, {legalField[], caseType[], situation[]})
 * - status: String (DRAFT / CONFIRMED)
 * - confirmedAt: LocalDateTime
 * - createdAt, updatedAt: LocalDateTime
 */
public class CaseStructure {
}
