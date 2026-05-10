package org.example.shield.consultation.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationReader;
import org.example.shield.consultation.exception.ConsultationNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConsultationReaderImpl implements ConsultationReader {

    private final ConsultationRepository consultationRepository;

    @Override
    public Consultation findById(UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ConsultationNotFoundException(id));
    }

    @Override
    public Page<Consultation> findAllByUserId(UUID userId, Pageable pageable) {
        return consultationRepository.findAllByUserId(userId, pageable);
    }
}
