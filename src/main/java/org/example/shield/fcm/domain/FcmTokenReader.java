package org.example.shield.fcm.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenReader {

    List<FcmToken> findAllByUserId(UUID userId);

    Optional<FcmToken> findByToken(String token);

    boolean existsByToken(String token);
}
