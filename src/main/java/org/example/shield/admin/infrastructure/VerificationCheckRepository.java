package org.example.shield.admin.infrastructure;

import org.example.shield.admin.domain.VerificationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCheckRepository extends JpaRepository<VerificationCheck, UUID> {
    Optional<VerificationCheck> findByLawyerId(UUID lawyerId);
}
