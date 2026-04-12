package org.example.shield.admin.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.admin.domain.VerificationLog;
import org.example.shield.admin.domain.VerificationLogReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class VerificationLogReaderImpl implements VerificationLogReader {

    private final VerificationLogRepository verificationLogRepository;

    @Override
    public Page<VerificationLog> findAll(Pageable pageable) {
        return verificationLogRepository.findAll(pageable);
    }

    @Override
    public Page<VerificationLog> findAllByToStatus(String toStatus, Pageable pageable) {
        return verificationLogRepository.findAllByToStatus(toStatus, pageable);
    }

    @Override
    public Page<VerificationLog> findAllByCreatedAtAfter(LocalDateTime after, Pageable pageable) {
        return verificationLogRepository.findAllByCreatedAtAfter(after, pageable);
    }

    @Override
    public Page<VerificationLog> findAllByToStatusAndCreatedAtAfter(String toStatus, LocalDateTime after,
                                                                     Pageable pageable) {
        return verificationLogRepository.findAllByToStatusAndCreatedAtAfter(toStatus, after, pageable);
    }
}
