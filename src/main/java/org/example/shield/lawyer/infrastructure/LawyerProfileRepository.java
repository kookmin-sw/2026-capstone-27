package org.example.shield.lawyer.infrastructure;

/**
 * 변호사 프로필 JPA Repository.
 *
 * TODO: extends JpaRepository<LawyerProfile, UUID>
 * - findByUserId(UUID userId): Optional<LawyerProfile>
 * - findByVerificationStatus(String status): List<LawyerProfile>
 * - findBySpecializationsContaining(String field): 전문분야로 검색 (매칭용)
 */
public interface LawyerProfileRepository {
}
