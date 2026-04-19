package org.example.shield.ai.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.type.SqlTypes;

/**
 * 법률 조문 청크 JPA 엔티티 (legal_chunks 테이블 매핑).
 *
 * <p>Flyway V3 마이그레이션으로 생성된 스키마에 대응한다.
 * DB 생성 컬럼(content_tsv: tsvector GENERATED)은 엔티티에 매핑하지 않으며,
 * 전문 검색은 Repository 네이티브 쿼리로 수행한다.</p>
 *
 * <p>category_ids, legislation_terms는 PostgreSQL text[] 타입으로,
 * Hibernate 6 {@link JdbcTypeCode}(SqlTypes.ARRAY)를 통해 {@code String[]}로 매핑한다.</p>
 *
 * <p>embedding은 pgvector {@code vector(1024)} 컬럼으로 Hibernate 6.5+ {@link SqlTypes#VECTOR}를 통해
 * {@code float[]}로 매핑된다. Flyway V4에서 추가되며 Phase B-2 인제스트 전까지는 NULL이다.</p>
 *
 * <p>부분 유니크 제약 {@code (law_id, article_no, chunk_index) WHERE abolition_date IS NULL}은
 * DB 레벨(Flyway V3)에서 관리하므로 JPA 유니크 제약으로 선언하지 않는다.</p>
 */
// 인덱스/유니크 제약은 Flyway V3 마이그레이션에서 관리 (JPA 중복 선언 지양)
@Entity
@Table(name = "legal_chunks")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "law_id", nullable = false, length = 50)
    private String lawId;

    @Column(name = "law_name", nullable = false, length = 200)
    private String lawName;

    @Column(name = "article_no", nullable = false, length = 50)
    private String articleNo;

    @Column(name = "chunk_index", nullable = false)
    private Short chunkIndex;

    @Column(name = "article_title", length = 300)
    private String articleTitle;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "abolition_date")
    private LocalDate abolitionDate;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "category_ids", columnDefinition = "text[]")
    private String[] categoryIds;

    @Column(name = "lod_uri", length = 500)
    private String lodUri;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "legislation_terms", columnDefinition = "text[]")
    private String[] legislationTerms;

    /**
     * Cohere embed-v4.0 (1024차원) 임베딩 벡터.
     * B-2 인제스트 파이프라인이 채우기 전에는 NULL.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    /**
     * 임베딩 생성에 사용된 모델 ID (예: "embed-v4.0"). 재임베딩 필요성 판단용.
     */
    @Column(name = "embedding_model", length = 64)
    private String embeddingModel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private LegalChunkEntity(String lawId,
                             String lawName,
                             String articleNo,
                             Short chunkIndex,
                             String articleTitle,
                             String content,
                             LocalDate effectiveDate,
                             LocalDate abolitionDate,
                             String sourceUrl,
                             String[] categoryIds,
                             String lodUri,
                             String[] legislationTerms,
                             float[] embedding,
                             String embeddingModel) {
        this.lawId = lawId;
        this.lawName = lawName;
        this.articleNo = articleNo;
        this.chunkIndex = chunkIndex;
        this.articleTitle = articleTitle;
        this.content = content;
        this.effectiveDate = effectiveDate;
        this.abolitionDate = abolitionDate;
        this.sourceUrl = sourceUrl;
        this.categoryIds = categoryIds;
        this.lodUri = lodUri;
        this.legislationTerms = legislationTerms;
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
    }

    /**
     * B-2 인제스트/재임베딩 파이프라인이 사용하는 업데이트라이터.
     * embedding과 embeddingModel은 항상 함께 갱신하여 정합성을 유지한다.
     */
    public void updateEmbedding(float[] embedding, String embeddingModel) {
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
    }
}
