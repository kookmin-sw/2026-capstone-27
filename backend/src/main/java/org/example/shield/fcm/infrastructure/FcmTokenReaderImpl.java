package org.example.shield.fcm.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.fcm.domain.FcmToken;
import org.example.shield.fcm.domain.FcmTokenReader;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FcmTokenReaderImpl implements FcmTokenReader {

    private final FcmTokenJpaRepository fcmTokenJpaRepository;

    @Override
    public List<FcmToken> findAllByUserId(UUID userId) {
        return fcmTokenJpaRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<FcmToken> findByToken(String token) {
        return fcmTokenJpaRepository.findByToken(token);
    }

    @Override
    public boolean existsByToken(String token) {
        return fcmTokenJpaRepository.existsByToken(token);
    }
}
