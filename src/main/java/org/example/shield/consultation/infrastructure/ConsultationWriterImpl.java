package org.example.shield.consultation.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.consultation.domain.Consultation;
import org.example.shield.consultation.domain.ConsultationWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConsultationWriterImpl implements ConsultationWriter {

    private final ConsultationRepository consultationRepository;

    @Override
    public Consultation save(Consultation consultation) {
        return consultationRepository.save(consultation);
    }
}
