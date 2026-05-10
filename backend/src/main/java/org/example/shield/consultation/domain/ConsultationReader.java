package org.example.shield.consultation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConsultationReader {

    Consultation findById(UUID id);

    Page<Consultation> findAllByUserId(UUID userId, Pageable pageable);
}
