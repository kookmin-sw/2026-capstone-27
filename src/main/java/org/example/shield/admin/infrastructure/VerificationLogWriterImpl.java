package org.example.shield.admin.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.admin.domain.VerificationLog;
import org.example.shield.admin.domain.VerificationLogWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VerificationLogWriterImpl implements VerificationLogWriter {

    private final VerificationLogRepository verificationLogRepository;

    @Override
    public VerificationLog save(VerificationLog log) {
        return verificationLogRepository.save(log);
    }
}
