package org.example.shield.brief.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BriefReader {
    Brief findById(UUID id);
    List<Brief> findAllByIds(List<UUID> ids);
    Page<Brief> findAllByUserId(UUID userId, Pageable pageable);
    Optional<Brief> findOptionalByConsultationId(UUID consultationId);
}
