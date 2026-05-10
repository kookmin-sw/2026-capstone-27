package org.example.shield.lawyer.infrastructure;

import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LawyerProfileRepository extends JpaRepository<LawyerProfile, UUID> {
    Optional<LawyerProfile> findByUserId(UUID userId);
    Page<LawyerProfile> findAllByVerificationStatus(VerificationStatus status, Pageable pageable);

    @Query(value = """
            SELECT lp.* FROM lawyers lp
            WHERE CAST(lp.verification_status AS TEXT) = 'VERIFIED'
            AND (CAST(:specialization AS TEXT) IS NULL OR LOWER(lp.domains::text) LIKE LOWER('%' || CAST(:specialization AS TEXT) || '%'))
            AND (CAST(:minExperience AS INTEGER) IS NULL OR lp.experience_years >= CAST(:minExperience AS INTEGER))
            """,
            countQuery = """
            SELECT COUNT(*) FROM lawyers lp
            WHERE CAST(lp.verification_status AS TEXT) = 'VERIFIED'
            AND (CAST(:specialization AS TEXT) IS NULL OR LOWER(lp.domains::text) LIKE LOWER('%' || CAST(:specialization AS TEXT) || '%'))
            AND (CAST(:minExperience AS INTEGER) IS NULL OR lp.experience_years >= CAST(:minExperience AS INTEGER))
            """,
            nativeQuery = true)
    Page<LawyerProfile> findVerifiedLawyers(
            @Param("specialization") String specialization,
            @Param("minExperience") Integer minExperience,
            Pageable pageable);

    @Query(value = """
            SELECT lp.* FROM lawyers lp
            JOIN users u ON u.id = lp.user_id
            WHERE (CAST(:status AS TEXT) IS NULL OR CAST(lp.verification_status AS TEXT) = :status)
            AND (CAST(:keyword AS TEXT) IS NULL
                 OR LOWER(u.name) LIKE LOWER('%' || CAST(:keyword AS TEXT) || '%')
                 OR LOWER(u.email) LIKE LOWER('%' || CAST(:keyword AS TEXT) || '%')
                 OR u.phone LIKE '%' || CAST(:keyword AS TEXT) || '%')
            ORDER BY lp.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM lawyers lp
            JOIN users u ON u.id = lp.user_id
            WHERE (CAST(:status AS TEXT) IS NULL OR CAST(lp.verification_status AS TEXT) = :status)
            AND (CAST(:keyword AS TEXT) IS NULL
                 OR LOWER(u.name) LIKE LOWER('%' || CAST(:keyword AS TEXT) || '%')
                 OR LOWER(u.email) LIKE LOWER('%' || CAST(:keyword AS TEXT) || '%')
                 OR u.phone LIKE '%' || CAST(:keyword AS TEXT) || '%')
            """,
            nativeQuery = true)
    Page<LawyerProfile> searchByStatusAndKeyword(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByVerificationStatus(VerificationStatus status);

    long countByVerificationStatusAndCreatedAtBefore(VerificationStatus status, LocalDateTime before);

    @Query(value = """
            SELECT COUNT(*) FROM lawyers lp
            WHERE NOT EXISTS (SELECT 1 FROM lawyer_documents ld WHERE ld.lawyer_id = lp.id)
            AND CAST(lp.verification_status AS TEXT) IN ('PENDING', 'REVIEWING', 'SUPPLEMENT_REQUESTED')
            """, nativeQuery = true)
    long countMissingDocuments();

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT bar_association_number FROM lawyers
                GROUP BY bar_association_number HAVING COUNT(*) > 1
            ) duplicates
            """, nativeQuery = true)
    long countDuplicateBarNumbers();
}
