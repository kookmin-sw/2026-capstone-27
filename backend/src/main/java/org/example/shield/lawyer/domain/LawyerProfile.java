package org.example.shield.lawyer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.shield.common.domain.BaseEntity;
import org.example.shield.common.enums.VerificationStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lawyers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LawyerProfile extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> domains;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> subDomains;

    private Integer experienceYears;

    @Column(nullable = false)
    private String barAssociationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "verification_status")
    private VerificationStatus verificationStatus;

    private LocalDateTime verifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(columnDefinition = "text")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> certifications;

    private Integer caseCount;

    @Column(length = 50)
    private String region;

    @Version
    private Long version;

    @Builder
    public LawyerProfile(UUID userId, String barAssociationNumber, List<String> domains,
                         List<String> subDomains, Integer experienceYears, List<String> tags,
                         String bio, List<String> certifications, String region) {
        this.userId = userId;
        this.barAssociationNumber = barAssociationNumber;
        this.domains = domains;
        this.subDomains = subDomains;
        this.experienceYears = experienceYears;
        this.tags = tags;
        this.bio = bio;
        this.certifications = certifications;
        this.region = region;
        this.verificationStatus = VerificationStatus.PENDING;
        this.caseCount = 0;
    }

    /**
     * 부분 업데이트 (PATCH semantics). null 인 필드는 기존 값을 유지한다.
     * 명시적 초기화가 필요하면 빈 컬렉션 또는 빈 문자열을 전달해야 한다.
     */
    public void updateProfile(List<String> domains, List<String> subDomains,
                              Integer experienceYears, List<String> certifications,
                              List<String> tags, String bio, String region) {
        if (domains != null) this.domains = domains;
        if (subDomains != null) this.subDomains = subDomains;
        if (experienceYears != null) this.experienceYears = experienceYears;
        if (certifications != null) this.certifications = certifications;
        if (tags != null) this.tags = tags;
        if (bio != null) this.bio = bio;
        if (region != null) this.region = region;
    }

    public void requestVerification(String barAssociationNumber) {
        this.barAssociationNumber = barAssociationNumber;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public void updateVerificationStatus(VerificationStatus newStatus) {
        this.verificationStatus = newStatus;
        if (newStatus == VerificationStatus.VERIFIED) {
            this.verifiedAt = LocalDateTime.now();
        }
    }
}
