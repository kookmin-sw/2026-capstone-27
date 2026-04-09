package org.example.shield.consultation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReader {

    List<Message> findAllByConsultationId(UUID consultationId);

    Page<Message> findAllByConsultationId(UUID consultationId, Pageable pageable);

    Optional<Message> findLastByConsultationId(UUID consultationId);
}
