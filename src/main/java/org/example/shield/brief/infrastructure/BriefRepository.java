package org.example.shield.brief.infrastructure;

import org.example.shield.brief.domain.Brief;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BriefRepository extends JpaRepository<Brief, UUID> {
    Page<Brief> findAllByUserId(UUID userId, Pageable pageable);
    Optional<Brief> findByConsultationId(UUID consultationId);
}
