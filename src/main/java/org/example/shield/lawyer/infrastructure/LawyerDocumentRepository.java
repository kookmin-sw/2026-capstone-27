package org.example.shield.lawyer.infrastructure;

import org.example.shield.lawyer.domain.LawyerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LawyerDocumentRepository extends JpaRepository<LawyerDocument, UUID> {
    List<LawyerDocument> findAllByLawyerId(UUID lawyerId);
}
