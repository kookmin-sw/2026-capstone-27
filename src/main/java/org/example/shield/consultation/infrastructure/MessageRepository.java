package org.example.shield.consultation.infrastructure;

import org.example.shield.consultation.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllByConsultationIdOrderBySequence(UUID consultationId);

    Page<Message> findAllByConsultationIdOrderBySequence(UUID consultationId, Pageable pageable);

    Optional<Message> findTopByConsultationIdOrderBySequenceDesc(UUID consultationId);
}
