package org.example.shield.admin.domain;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCheckReader {
    Optional<VerificationCheck> findOptionalByLawyerId(UUID lawyerId);
}
