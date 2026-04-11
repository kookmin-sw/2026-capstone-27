package org.example.shield.lawyer.infrastructure;

import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LawyerProfileRepository extends JpaRepository<LawyerProfile, UUID> {
    Optional<LawyerProfile> findByUserId(UUID userId);
    Page<LawyerProfile> findAllByVerificationStatus(VerificationStatus status, Pageable pageable);
}
