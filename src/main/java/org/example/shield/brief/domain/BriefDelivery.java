package org.example.shield.brief.domain;

/**
 * 의뢰서 전달 엔티티 - brief_deliveries 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - briefId: UUID (FK → briefs.id)
 * - lawyerId: UUID (FK → users.id)
 * - status: String (SENT/VIEWED/ACCEPTED/REJECTED/NO_RESPONSE)
 * - rejectionReason: String (nullable)
 * - sentAt: LocalDateTime
 * - viewedAt: LocalDateTime (nullable)
 * - respondedAt: LocalDateTime (nullable)
 */
public class BriefDelivery {
}
