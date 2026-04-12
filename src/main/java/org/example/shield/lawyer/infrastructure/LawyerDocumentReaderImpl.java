package org.example.shield.lawyer.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.lawyer.domain.LawyerDocument;
import org.example.shield.lawyer.domain.LawyerDocumentReader;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LawyerDocumentReaderImpl implements LawyerDocumentReader {

    private final LawyerDocumentRepository lawyerDocumentRepository;

    @Override
    public List<LawyerDocument> findAllByLawyerId(UUID lawyerId) {
        return lawyerDocumentRepository.findAllByLawyerId(lawyerId);
    }
}
