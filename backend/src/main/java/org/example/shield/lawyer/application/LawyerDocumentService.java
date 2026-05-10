package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.common.storage.StorageClient;
import org.example.shield.lawyer.controller.dto.DocumentResponse;
import org.example.shield.lawyer.domain.LawyerDocument;
import org.example.shield.lawyer.domain.LawyerDocumentReader;
import org.example.shield.lawyer.domain.LawyerDocumentWriter;
import org.example.shield.lawyer.domain.LawyerReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerDocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("PDF", "JPG", "JPEG", "PNG");

    private final StorageClient storageClient;
    private final LawyerReader lawyerReader;
    private final LawyerDocumentReader lawyerDocumentReader;
    private final LawyerDocumentWriter lawyerDocumentWriter;

    @Transactional
    public DocumentResponse uploadDocument(UUID userId, MultipartFile file) {
        log.info("서류 업로드 요청. userId={}, fileName={}", userId, file.getOriginalFilename());

        validateFile(file);

        var lawyer = lawyerReader.findByUserId(userId);
        String fileType = extractFileType(file.getOriginalFilename());
        String sanitizedName = sanitizeFileName(file.getOriginalFilename());
        String storagePath = lawyer.getId() + "/" + UUID.randomUUID() + "_" + sanitizedName;

        String filePath = storageClient.upload(storagePath, file);

        LawyerDocument document = LawyerDocument.create(
                lawyer.getId(),
                file.getOriginalFilename(),
                filePath,
                file.getSize(),
                fileType
        );
        LawyerDocument saved = lawyerDocumentWriter.save(document);

        String signedUrl = storageClient.getSignedUrl(filePath, 3600);

        log.info("서류 업로드 완료. documentId={}, lawyerId={}", saved.getId(), lawyer.getId());
        return DocumentResponse.fromWithUrl(saved, signedUrl);
    }

    private static final int SIGNED_URL_EXPIRY = 3600;

    public List<DocumentResponse> getDocuments(UUID lawyerId) {
        log.info("서류 목록 조회. lawyerId={}", lawyerId);
        return lawyerDocumentReader.findAllByLawyerId(lawyerId).stream()
                .map(doc -> {
                    String signedUrl = storageClient.getSignedUrl(doc.getFileUrl(), SIGNED_URL_EXPIRY);
                    return DocumentResponse.fromWithUrl(doc, signedUrl);
                })
                .toList();
    }

    public List<DocumentResponse> getMyDocuments(UUID userId) {
        var lawyer = lawyerReader.findByUserId(userId);
        return getDocuments(lawyer.getId());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE) {};
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.DOCUMENT_SIZE_EXCEEDED) {};
        }
        String fileType = extractFileType(file.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(fileType.toUpperCase())) {
            throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED) {};
        }
    }

    /**
     * 파일명을 storage path 안전 형태로 정제.
     * URI.create() 는 non-ASCII(한글 등) 문자를 허용하지 않으므로 영숫자·점·대시·언더스코어만 유지한다.
     * 원본 파일명은 LawyerDocument.fileName 컬럼에 별도 보존되므로 UI 표시는 영향 없음.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED) {};
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
    }
}
