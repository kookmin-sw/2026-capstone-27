package org.example.shield.consultation.domain;

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
import org.example.shield.common.enums.ConsultationStatus;
import org.example.shield.common.enums.DomainType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consultations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultation extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "consultation_status")
    private ConsultationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "domain_type")
    private DomainType selectedDomain;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> primaryField;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(columnDefinition = "text")
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    /**
     * LLM 응답 completion ID (감사 로깅용).
     * Cohere v2 Chat API는 무상태 모델이므로 Stateful 연결 용도 없음 — 항상 full history 전송.
     * 과거 LLM 제공자의 previous_response_id 호환을 위해 필드는 보존 (DB 스키마 호환성).
     */
    @Column(columnDefinition = "text")
    private String lastResponseId;

    /**
     * primary_field_locked 플래그 (P0-V).
     * 사용자가 PATCH /classify로 분류를 수정하면 true → LLM override 방지.
     */
    @Column(nullable = false)
    private boolean primaryFieldLocked = false;

    @Builder
    private Consultation(UUID userId, DomainType selectedDomain) {
        this.userId = userId;
        this.selectedDomain = selectedDomain;
        this.status = ConsultationStatus.COLLECTING;
    }

    public static Consultation create(UUID userId, DomainType selectedDomain) {
        return Consultation.builder()
                .userId(userId)
                .selectedDomain(selectedDomain)
                .build();
    }

    public void updateClassification(List<String> primaryField) {
        this.primaryField = primaryField;
        this.primaryFieldLocked = true;  // P0-V: 사용자 수정 시 lock
        this.lastResponseId = null;      // no-op post-Cohere migration (full history 모드)
    }

    /**
     * LLM 분류 결과 반영 (locked가 아닐 때만).
     */
    public boolean updateClassificationFromLlm(List<String> primaryField) {
        if (this.primaryFieldLocked) {
            return false;  // locked — 무시
        }
        this.primaryField = primaryField;
        this.lastResponseId = null;  // no-op post-Cohere migration (full history 모드)
        return true;
    }

    public void updateTags(List<String> tags) {
        this.tags = tags;
    }

    public void updateLastResponseId(String responseId) {
        this.lastResponseId = responseId;
    }

    public void updateStatus(ConsultationStatus status) {
        this.status = status;
    }

    public void updateLastMessage(String content, LocalDateTime timestamp) {
        this.lastMessage = content;
        this.lastMessageAt = timestamp;
    }
}
