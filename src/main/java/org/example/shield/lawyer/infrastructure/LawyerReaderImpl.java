package org.example.shield.lawyer.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.common.enums.VerificationStatus;
import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;
import org.example.shield.lawyer.domain.LawyerProfile;
import org.example.shield.lawyer.domain.LawyerReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LawyerReaderImpl implements LawyerReader {

    private final LawyerProfileRepository lawyerProfileRepository;

    @Override
    public LawyerProfile findById(UUID id) {
        return lawyerProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LAWYER_NOT_FOUND) {});
    }

    @Override
    public LawyerProfile findByUserId(UUID userId) {
        return lawyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LAWYER_NOT_FOUND) {});
    }

    @Override
    public Page<LawyerProfile> findAllByVerificationStatus(VerificationStatus status, Pageable pageable) {
        return lawyerProfileRepository.findAllByVerificationStatus(status, pageable);
    }
}
