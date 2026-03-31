package org.example.shield.lawyer.domain;

/**
 * 변호사 프로필 엔티티 - lawyer_profiles 테이블 매핑.
 *
 * TODO: @Entity 구현
 * - id: UUID (PK)
 * - userId: UUID (FK → users.id, UNIQUE)
 * - specializations: List<String> (JSONB, 전문분야)
 * - experienceYears: int
 * - certifications: String
 * - barAssociationNumber: String (변협 등록번호)
 * - verificationStatus: String (PENDING / APPROVED / REJECTED)
 * - verifiedAt: LocalDateTime
 * - createdAt, updatedAt: LocalDateTime
 */
public class LawyerProfile {
}
