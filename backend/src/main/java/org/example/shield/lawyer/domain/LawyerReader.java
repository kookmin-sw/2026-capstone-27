package org.example.shield.lawyer.domain;

import org.example.shield.common.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LawyerReader {
    LawyerProfile findById(UUID id);
    LawyerProfile findByUserId(UUID userId);
    Page<LawyerProfile> findAllByVerificationStatus(VerificationStatus status, Pageable pageable);
    Page<LawyerProfile> findVerifiedLawyers(String specialization, Integer minExperience, Pageable pageable);
    Page<LawyerProfile> searchByStatusAndKeyword(String status, String keyword, Pageable pageable);
    long countByVerificationStatus(VerificationStatus status);
}
