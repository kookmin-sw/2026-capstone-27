package org.example.shield.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * <p>부분 유니크 제약 {@code (law_id, article_no, chunk_index) WHERE abolition_date IS NULL}은
 * DB 레벨(Flyway V3)에서 관리하므로 JPA 유니크 제약으로 선언하지 않는다.</p>
 */
@Entity
@Table(
        name = "legal_chunks",
        indexes = {
                @Index(name = "idx_legal_chunks_law_id", columnList = "law_id")
        }
)
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
                             String[] legislationTerms) {
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
    }
}
