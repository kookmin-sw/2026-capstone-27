package org.example.shield.consultation.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.consultation.domain.Message;
import org.example.shield.consultation.domain.MessageWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MessageWriterImpl implements MessageWriter {

    private final MessageRepository messageRepository;

    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }
}
