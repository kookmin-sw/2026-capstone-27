package org.example.shield.lawyer.infrastructure;

import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LawyerProfileRepository extends JpaRepository<LawyerProfile, UUID> {
    Optional<LawyerProfile> findByUserId(UUID userId);
    Page<LawyerProfile> findAllByVerificationStatus(VerificationStatus status, Pageable pageable);

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
}
