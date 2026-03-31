package org.example.shield.brief.domain;

/**
 * 의뢰서 엔티티 - briefs 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - consultationId: UUID (FK → consultations.id)
 * - userId: UUID (FK → users.id)
 * - title: String (자동 생성, "[노동] 부당해고 관련 의뢰서")
 * - legalField: String
 * - content: String (TEXT, Grok Expert가 생성한 줄글 의뢰서)
 * - keywords: List<String> (JSONB, 변호사 매칭용)
 * - privacySetting: String (FULL/PARTIAL/PRIVATE, 기본 PARTIAL)
 * - status: String (DRAFT/CONFIRMED/DELIVERED/DISCARDED)
 * - confirmedAt: LocalDateTime
 * - createdAt, updatedAt: LocalDateTime
 */
public class Brief {
}
