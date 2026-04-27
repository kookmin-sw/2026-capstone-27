package org.example.shield.fcm.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.shield.fcm.domain.FcmToken;
import org.example.shield.fcm.domain.FcmTokenWriter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class FcmTokenWriterImpl implements FcmTokenWriter {

    private final FcmTokenJpaRepository fcmTokenJpaRepository;

    @Override
    public FcmToken save(FcmToken token) {
        return fcmTokenJpaRepository.save(token);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        fcmTokenJpaRepository.deleteByToken(token);
    }
}
