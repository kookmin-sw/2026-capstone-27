package org.example.shield.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_checks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID lawyerId;

    @Column(nullable = false)
    private boolean emailDuplicate;

    @Column(nullable = false)
    private boolean phoneDuplicate;

    @Column(nullable = false)
    private boolean nameDuplicate;

    @Column(nullable = false)
    private boolean requiredFields;

    @Column(nullable = false)
    private boolean licenseVerified;

    @Column(nullable = false)
    private boolean documentMatched;

    @Column(nullable = false)
    private boolean specializationValid;

    @Column(nullable = false)
    private boolean experienceVerified;

    @Column(nullable = false)
    private boolean duplicateSignup;

    @Column(nullable = false)
    private boolean documentComplete;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static VerificationCheck create(UUID lawyerId) {
        VerificationCheck check = new VerificationCheck();
        check.lawyerId = lawyerId;
        check.updatedAt = LocalDateTime.now();
        return check;
    }

    public void updateAutoChecks(boolean emailDuplicate, boolean phoneDuplicate,
                                 boolean nameDuplicate, boolean requiredFields) {
        this.emailDuplicate = emailDuplicate;
        this.phoneDuplicate = phoneDuplicate;
        this.nameDuplicate = nameDuplicate;
        this.requiredFields = requiredFields;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateManualChecks(boolean licenseVerified, boolean documentMatched,
                                   boolean specializationValid, boolean experienceVerified,
                                   boolean duplicateSignup, boolean documentComplete) {
        this.licenseVerified = licenseVerified;
        this.documentMatched = documentMatched;
        this.specializationValid = specializationValid;
        this.experienceVerified = experienceVerified;
        this.duplicateSignup = duplicateSignup;
        this.documentComplete = documentComplete;
        this.updatedAt = LocalDateTime.now();
    }

    public int getCompletedCount() {
        int count = 0;
        if (emailDuplicate) count++;
        if (phoneDuplicate) count++;
        if (nameDuplicate) count++;
        if (requiredFields) count++;
        if (licenseVerified) count++;
        if (documentMatched) count++;
        if (specializationValid) count++;
        if (experienceVerified) count++;
        if (duplicateSignup) count++;
        if (documentComplete) count++;
        return count;
    }

    public int getTotalCount() {
        return 10;
    }
}
