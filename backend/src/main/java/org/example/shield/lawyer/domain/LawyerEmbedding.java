package org.example.shield.lawyer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 변호사 임베딩 엔티티 (Issue #50, lawyer_embeddings 테이블 매핑).
 *
 * <p>Flyway V8 마이그레이션으로 생성된 {@code lawyer_embeddings} 와 1:1 매핑.
 * {@code lawyer_id} 가 PK 이자 {@code lawyers.id} 의 FK (ON DELETE CASCADE).</p>
 *
 * <p>문서 벡터는 {@link LawyerEmbeddingTextBuilder} 로 조립한 텍스트를
 * Cohere embed-v4.0 (1024차원) 에 태워 생성한다. 재임베딩 판단은 {@code sourceHash}
 * (SHA-256) 로 비교.</p>
 *
 * <p>LegalChunkEntity 와 동일한 {@code SqlTypes.VECTOR} + {@code float[]} 매핑 패턴.</p>
 */
@Entity
@Table(name = "lawyer_embeddings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LawyerEmbedding {

    @Id
    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(name = "embedding_model", nullable = false, length = 64)
    private String embeddingModel;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Column(name = "source_text", columnDefinition = "text")
    private String sourceText;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 신규 임베딩 생성.
     */
    public static LawyerEmbedding create(UUID lawyerId,
                                         float[] embedding,
                                         String embeddingModel,
                                         String sourceHash,
                                         String sourceText) {
        LawyerEmbedding e = new LawyerEmbedding();
        e.lawyerId = lawyerId;
        e.embedding = embedding;
        e.embeddingModel = embeddingModel;
        e.sourceHash = sourceHash;
        e.sourceText = sourceText;
        return e;
    }

    /**
     * 재임베딩 (프로필 변경 또는 모델 교체).
     */
    public void updateEmbedding(float[] embedding,
                                String embeddingModel,
                                String sourceHash,
                                String sourceText) {
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
        this.sourceHash = sourceHash;
        this.sourceText = sourceText;
    }
}
