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
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 판례(legal_cases) JPA 엔티티.
 *
 * <p>Flyway V6 마이그레이션으로 생성된 스키마에 대응한다. legal_chunks와 달리 판례는 1건 = 1 row.
 * headnote(판시사항), holding(판결요지), reasoning(판결이유), full_text(원문)을 본문 필드로 분리
 * 보관하고, 임베딩 입력은 C-4 인제스트 파이프라인이 "headnote + holding" 조합으로 생성한다.</p>
 *
 * <p>DB 생성 컬럼 {@code content_tsv}(tsvector GENERATED)는 매핑하지 않으며, 전문검색은 Repository
 * 네이티브 쿼리로 수행한다. 배열 컬럼(cited_articles, cited_cases, category_ids)은 Hibernate 6
 * {@link JdbcTypeCode}(SqlTypes.ARRAY)로 {@code String[]}에 매핑. embedding은 pgvector
 * {@code vector(1024)} 컬럼에 {@link SqlTypes#VECTOR}로 매핑.</p>
 *
 * <p>자연키 유니크 제약 {@code (case_no, court, decision_date)}은 DB 레벨(Flyway V6)에서 관리한다.</p>
 */
@Entity
@Table(name = "legal_cases")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_no", nullable = false, length = 64)
    private String caseNo;

    @Column(name = "court", nullable = false, length = 100)
    private String court;

    @Column(name = "case_name", length = 500)
    private String caseName;

    @Column(name = "decision_date", nullable = false)
    private LocalDate decisionDate;

    @Column(name = "case_type", nullable = false, length = 32)
    private String caseType;

    @Column(name = "judgment_type", length = 32)
    private String judgmentType;

    @Column(name = "disposition", length = 200)
    private String disposition;

    @Column(name = "headnote", columnDefinition = "text")
    private String headnote;

    @Column(name = "holding", columnDefinition = "text")
    private String holding;

    @Column(name = "reasoning", columnDefinition = "text")
    private String reasoning;

    @Column(name = "full_text", columnDefinition = "text")
    private String fullText;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cited_articles", columnDefinition = "text[]")
    private String[] citedArticles;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cited_cases", columnDefinition = "text[]")
    private String[] citedCases;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "category_ids", columnDefinition = "text[]")
    private String[] categoryIds;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "source_id", length = 128)
    private String sourceId;

    /**
     * Cohere embed-v4.0 (1024차원) 임베딩. C-4 인제스트 파이프라인이 채우기 전에는 NULL.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(name = "embedding_model", length = 64)
    private String embeddingModel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private LegalCaseEntity(String caseNo,
                            String court,
                            String caseName,
                            LocalDate decisionDate,
                            String caseType,
                            String judgmentType,
                            String disposition,
                            String headnote,
                            String holding,
                            String reasoning,
                            String fullText,
                            String[] citedArticles,
                            String[] citedCases,
                            String[] categoryIds,
                            String source,
                            String sourceUrl,
                            String sourceId,
                            float[] embedding,
                            String embeddingModel) {
        this.caseNo = caseNo;
        this.court = court;
        this.caseName = caseName;
        this.decisionDate = decisionDate;
        this.caseType = caseType;
        this.judgmentType = judgmentType;
        this.disposition = disposition;
        this.headnote = headnote;
        this.holding = holding;
        this.reasoning = reasoning;
        this.fullText = fullText;
        this.citedArticles = citedArticles;
        this.citedCases = citedCases;
        this.categoryIds = categoryIds;
        this.source = source != null ? source : "law.go.kr";
        this.sourceUrl = sourceUrl;
        this.sourceId = sourceId;
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
    }

    /**
     * C-4 인제스트/재임베딩 파이프라인이 사용하는 업데이터.
     * embedding과 embeddingModel은 항상 함께 갱신하여 정합성을 유지한다.
     */
    public void updateEmbedding(float[] embedding, String embeddingModel) {
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 본문 필드가 업데이트될 때(재수집) 호출. embedding은 별도 경로로 갱신.
     */
    public void updateContent(String caseName,
                              String headnote,
                              String holding,
                              String reasoning,
                              String fullText,
                              String[] citedArticles,
                              String[] citedCases,
                              String[] categoryIds,
                              String disposition,
                              String judgmentType) {
        this.caseName = caseName;
        this.headnote = headnote;
        this.holding = holding;
        this.reasoning = reasoning;
        this.fullText = fullText;
        this.citedArticles = citedArticles;
        this.citedCases = citedCases;
        this.categoryIds = categoryIds;
        this.disposition = disposition;
        this.judgmentType = judgmentType;
    }
}
