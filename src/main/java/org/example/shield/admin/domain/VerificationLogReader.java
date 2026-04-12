package org.example.shield.admin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VerificationLogReader {
    Page<VerificationLog> findAll(Pageable pageable);
    Page<VerificationLog> findAllByToStatus(String toStatus, Pageable pageable);
    Page<VerificationLog> findAllByCreatedAtAfter(LocalDateTime after, Pageable pageable);
    Page<VerificationLog> findAllByToStatusAndCreatedAtAfter(String toStatus, LocalDateTime after, Pageable pageable);
}
