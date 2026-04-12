package org.example.shield.admin.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.admin.domain.VerificationCheck;
import org.example.shield.admin.domain.VerificationCheckReader;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VerificationCheckReaderImpl implements VerificationCheckReader {

    private final VerificationCheckRepository verificationCheckRepository;

    @Override
    public Optional<VerificationCheck> findOptionalByLawyerId(UUID lawyerId) {
        return verificationCheckRepository.findByLawyerId(lawyerId);
    }
}
