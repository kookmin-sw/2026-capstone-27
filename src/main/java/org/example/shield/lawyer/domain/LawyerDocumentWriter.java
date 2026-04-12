package org.example.shield.lawyer.domain;

public interface LawyerDocumentWriter {
    LawyerDocument save(LawyerDocument document);
    void deleteById(java.util.UUID id);
}
