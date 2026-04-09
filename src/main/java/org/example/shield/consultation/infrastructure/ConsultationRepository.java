package org.example.shield.consultation.infrastructure;

import org.example.shield.consultation.domain.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Page<Consultation> findAllByUserId(UUID userId, Pageable pageable);
}
