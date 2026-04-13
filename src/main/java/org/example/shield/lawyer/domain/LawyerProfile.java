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

    private String specializations;

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
    public LawyerProfile(UUID userId, String barAssociationNumber, String specializations,
                         Integer experienceYears, List<String> tags, String bio,
                         List<String> certifications, String region) {
        this.userId = userId;
        this.barAssociationNumber = barAssociationNumber;
        this.specializations = specializations;
        this.experienceYears = experienceYears;
        this.tags = tags;
        this.bio = bio;
        this.certifications = certifications;
        this.region = region;
        this.verificationStatus = VerificationStatus.PENDING;
        this.caseCount = 0;
    }

    public void updateProfile(String specializations, Integer experienceYears,
                              List<String> certifications, List<String> tags,
                              String bio, String region) {
        this.specializations = specializations;
        this.experienceYears = experienceYears;
        this.certifications = certifications;
        this.tags = tags;
        this.bio = bio;
        this.region = region;
    }

    public void updateVerificationStatus(VerificationStatus newStatus) {
        this.verificationStatus = newStatus;
        if (newStatus == VerificationStatus.VERIFIED) {
            this.verifiedAt = LocalDateTime.now();
        }
    }
}
