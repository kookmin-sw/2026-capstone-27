package org.example.shield.lawyer.controller.dto;

import org.example.shield.lawyer.domain.LawyerDocument;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID documentId,
        String fileName,
        Long fileSize,
        String fileType,
        String fileUrl,
        LocalDateTime createdAt
) {
    public static DocumentResponse fromWithUrl(LawyerDocument document, String signedUrl) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getFileSize(),
                document.getFileType(),
                signedUrl,
                document.getCreatedAt()
        );
    }
}
