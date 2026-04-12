package org.example.shield.lawyer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lawyer_documents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LawyerDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID lawyerId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 20)
    private String fileType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private LawyerDocument(UUID lawyerId, String fileName, String fileUrl,
                           Long fileSize, String fileType) {
        this.lawyerId = lawyerId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.fileType = fileType;
    }

    public static LawyerDocument create(UUID lawyerId, String fileName, String fileUrl,
                                        Long fileSize, String fileType) {
        return LawyerDocument.builder()
                .lawyerId(lawyerId)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .fileType(fileType)
                .build();
    }
}
