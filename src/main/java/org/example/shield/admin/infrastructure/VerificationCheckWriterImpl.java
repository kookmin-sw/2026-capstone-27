package org.example.shield.admin.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.admin.domain.VerificationCheck;
import org.example.shield.admin.domain.VerificationCheckWriter;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VerificationCheckWriterImpl implements VerificationCheckWriter {

    private final VerificationCheckRepository verificationCheckRepository;

    @Override
    public VerificationCheck save(VerificationCheck check) {
        return verificationCheckRepository.save(check);
    }
}
