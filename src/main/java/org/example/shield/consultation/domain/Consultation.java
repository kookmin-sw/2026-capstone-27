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
    }

    public void updateStatus(ConsultationStatus status) {
        this.status = status;
    }
}
