package org.example.shield.consultation.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageReaderImpl implements MessageReader {

    private final MessageRepository messageRepository;

    @Override
    public List<Message> findAllByConsultationId(UUID consultationId) {
        return messageRepository.findAllByConsultationIdOrderBySequence(consultationId);
    }

    @Override
    public Page<Message> findAllByConsultationId(UUID consultationId, Pageable pageable) {
        return messageRepository.findAllByConsultationIdOrderBySequence(consultationId, pageable);
    }

    @Override
    public Optional<Message> findLastByConsultationId(UUID consultationId) {
        return messageRepository.findTopByConsultationIdOrderBySequenceDesc(consultationId);
    }
}
