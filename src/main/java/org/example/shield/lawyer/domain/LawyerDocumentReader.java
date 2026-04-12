package org.example.shield.lawyer.domain;

import java.util.List;
import java.util.UUID;

public interface LawyerDocumentReader {
    List<LawyerDocument> findAllByLawyerId(UUID lawyerId);
}
