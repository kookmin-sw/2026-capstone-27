package org.example.shield.admin.infrastructure;

import org.example.shield.admin.domain.VerificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, UUID> {
    Page<VerificationLog> findAllByToStatus(String toStatus, Pageable pageable);
    Page<VerificationLog> findAllByCreatedAtAfter(LocalDateTime after, Pageable pageable);
    Page<VerificationLog> findAllByToStatusAndCreatedAtAfter(String toStatus, LocalDateTime after, Pageable pageable);
    long countByCreatedAtAfterAndToStatusIn(LocalDateTime after, java.util.List<String> statuses);
}
