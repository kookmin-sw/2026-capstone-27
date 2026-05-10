package org.example.shield.lawyer.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.lawyer.domain.LawyerDocument;
import org.example.shield.lawyer.domain.LawyerDocumentWriter;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LawyerDocumentWriterImpl implements LawyerDocumentWriter {

    private final LawyerDocumentRepository lawyerDocumentRepository;

    @Override
    public LawyerDocument save(LawyerDocument document) {
        return lawyerDocumentRepository.save(document);
    }

    @Override
    public void deleteById(UUID id) {
        lawyerDocumentRepository.deleteById(id);
    }
}
