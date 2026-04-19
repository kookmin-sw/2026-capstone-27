package org.example.shield.consultation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.domain.BaseEntity;
import org.example.shield.common.enums.ConsultationStatus;
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

    // ── 사용자 선택 (상담 생성 시) ──

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> userDomains;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> userSubDomains;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> userTags;

    // ── AI 분류 (대화 중 LLM이 판단) ──

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> aiDomains;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> aiSubDomains;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> aiTags;

    // ── 공통 필드 ──

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

    private Consultation(UUID userId, List<String> domains, List<String> subDomains, List<String> tags) {
        this.userId = userId;
        this.userDomains = domains;
        this.userSubDomains = subDomains;
        this.userTags = tags;
        this.status = ConsultationStatus.COLLECTING;
    }

    public static Consultation create(UUID userId, List<String> domains,
                                      List<String> subDomains, List<String> tags) {
        return new Consultation(userId, domains, subDomains, tags);
    }

    /**
     * 사용자가 직접 분류를 수정.
     */
    public void updateUserClassification(List<String> domains, List<String> subDomains,
                                         List<String> tags) {
        this.userDomains = domains;
        this.userSubDomains = subDomains;
        this.userTags = tags;
    }

    /**
     * LLM 분류 결과를 반영한다.
     * 사용자가 이미 선택한 레벨(userDomains/SubDomains/Tags 가 비어있지 않은 레벨)
     * 은 잠기고, 비워둔 레벨만 AI 가 채운다 (per-level lock, Issue #48).
     *
     * @return 실제로 하나라도 업데이트되었으면 true
     */
    public boolean updateAiClassification(List<String> domains, List<String> subDomains,
                                          List<String> tags) {
        boolean anyUpdated = false;
        if (!isNonEmpty(this.userDomains) && domains != null) {
            this.aiDomains = domains;
            anyUpdated = true;
        }
        if (!isNonEmpty(this.userSubDomains) && subDomains != null) {
            this.aiSubDomains = subDomains;
            anyUpdated = true;
        }
        if (!isNonEmpty(this.userTags) && tags != null) {
            this.aiTags = tags;
            anyUpdated = true;
        }
        return anyUpdated;
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

    /**
     * 도메인 정보 추출: userDomains 우선, aiDomains 폴백.
     * 온톨로지 L1 한글 이름을 담는다 (예: "부동산 거래").
     */
    public String getFirstDomain() {
        if (isNonEmpty(userDomains)) return userDomains.get(0);
        if (isNonEmpty(aiDomains)) return aiDomains.get(0);
        return null;
    }

    /**
     * 서브도메인(L2) 추출: userSubDomains 우선, aiSubDomains 폴백.
     * 온톨로지 L2 한글 이름을 담는다 (예: "부동산 매매").
     */
    public String getFirstSubDomain() {
        if (isNonEmpty(userSubDomains)) return userSubDomains.get(0);
        if (isNonEmpty(aiSubDomains)) return aiSubDomains.get(0);
        return null;
    }

    /**
     * 태그(L3) 추출: userTags 우선, aiTags 폴백.
     * 온톨로지 L3 한글 이름을 담는다 (예: "매매 계약 불이행").
     */
    public String getFirstTag() {
        if (isNonEmpty(userTags)) return userTags.get(0);
        if (isNonEmpty(aiTags)) return aiTags.get(0);
        return null;
    }

    private static boolean isNonEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }
}
