package org.example.shield.fcm.infrastructure;

import org.example.shield.fcm.domain.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenJpaRepository extends JpaRepository<FcmToken, UUID> {

    List<FcmToken> findAllByUserId(UUID userId);

    Optional<FcmToken> findByToken(String token);

    boolean existsByToken(String token);

    void deleteByToken(String token);
}
