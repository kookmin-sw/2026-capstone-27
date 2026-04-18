package org.example.shield.lawyer.application;

import lombok.RequiredArgsConstructor;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.lawyer.controller.dto.LawyerRegisterRequest;
import org.example.shield.lawyer.controller.dto.VerificationResponse;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.example.shield.lawyer.domain.LawyerWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationService {

    private final LawyerReader lawyerReader;
    private final LawyerWriter lawyerWriter;

    /**
     * 변호사 가입: 추가정보 입력 + 검증 신청 통합.
     */
    public VerificationResponse register(UUID userId, LawyerRegisterRequest request) {
        // 이미 가입된 변호사인지 확인
        try {
            lawyerReader.findByUserId(userId);
            throw new BusinessException(ErrorCode.VERIFICATION_ALREADY_SUBMITTED) {};
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.LAWYER_NOT_FOUND) {
                throw e;
            }
        }

        LawyerProfile profile = LawyerProfile.builder()
                .userId(userId)
                .barAssociationNumber(request.barAssociationNumber())
                .domains(request.domains())
                .subDomains(request.subDomains())
                .tags(request.tags())
                .experienceYears(request.experienceYears())
                .bio(request.bio())
                .region(request.region())
                .build();

        LawyerProfile saved = lawyerWriter.save(profile);
        return VerificationResponse.from(saved);
    }

    public VerificationResponse requestVerification(UUID userId, String barAssociationNumber) {
        try {
            LawyerProfile existing = lawyerReader.findByUserId(userId);
            if (existing.getVerificationStatus() == VerificationStatus.PENDING
                    || existing.getVerificationStatus() == VerificationStatus.REVIEWING) {
                throw new BusinessException(ErrorCode.VERIFICATION_ALREADY_SUBMITTED) {};
            }
            existing.requestVerification(barAssociationNumber);
            return VerificationResponse.from(existing);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.LAWYER_NOT_FOUND) {
                LawyerProfile profile = LawyerProfile.builder()
                        .userId(userId)
                        .barAssociationNumber(barAssociationNumber)
                        .build();
                LawyerProfile saved = lawyerWriter.save(profile);
                return VerificationResponse.from(saved);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public VerificationResponse getVerificationStatus(UUID userId) {
        LawyerProfile profile = lawyerReader.findByUserId(userId);
        return VerificationResponse.from(profile);
    }
}
