package org.example.shield.brief.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.domain.BaseEntity;
import org.example.shield.common.enums.BriefStatus;
import org.example.shield.common.enums.PrivacySetting;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "briefs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brief extends BaseEntity {

    @Column(nullable = false)
    private UUID consultationId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String legalField;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "privacy_setting")
    private PrivacySetting privacySetting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "brief_status")
    private BriefStatus status;

    private LocalDateTime confirmedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<KeyIssue> keyIssues;

    @Column(columnDefinition = "text")
    private String strategy;

    @Builder
    private Brief(UUID consultationId, UUID userId, String title, String legalField,
                  String content, List<String> keywords, List<KeyIssue> keyIssues, String strategy) {
        this.consultationId = consultationId;
        this.userId = userId;
        this.title = title;
        this.legalField = legalField;
        this.content = content;
        this.keywords = keywords;
        this.keyIssues = keyIssues;
        this.strategy = strategy;
        this.privacySetting = PrivacySetting.PARTIAL;
        this.status = BriefStatus.DRAFT;
    }

    public static Brief create(UUID consultationId, UUID userId, String title, String legalField,
                               String content, List<String> keywords, List<KeyIssue> keyIssues, String strategy) {
        return Brief.builder()
                .consultationId(consultationId)
                .userId(userId)
                .title(title)
                .legalField(legalField)
                .content(content)
                .keywords(keywords)
                .keyIssues(keyIssues)
                .strategy(strategy)
                .build();
    }

    public void updateContent(String title, String content, List<String> keywords,
                              List<KeyIssue> keyIssues, String strategy) {
        this.title = title;
        this.content = content;
        this.keywords = keywords;
        this.keyIssues = keyIssues;
        this.strategy = strategy;
    }

    public void updatePrivacySetting(PrivacySetting privacySetting) {
        this.privacySetting = privacySetting;
    }

    public void confirm() {
        this.status = BriefStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void markDelivered() {
        this.status = BriefStatus.DELIVERED;
    }

    public void discard() {
        this.status = BriefStatus.DISCARDED;
    }

    public boolean isEditable() {
        return this.status == BriefStatus.DRAFT;
    }
}
