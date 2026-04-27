package org.example.shield.fcm.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.fcm.domain.DeviceType;
import org.example.shield.fcm.domain.FcmToken;
import org.example.shield.fcm.domain.FcmTokenReader;
import org.example.shield.fcm.domain.FcmTokenWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmTokenService {

    private final FcmTokenReader fcmTokenReader;
    private final FcmTokenWriter fcmTokenWriter;

    @Transactional
    public void register(UUID userId, String token, DeviceType deviceType) {
        Optional<FcmToken> existing = fcmTokenReader.findByToken(token);
        if (existing.isPresent()) {
            FcmToken fcmToken = existing.get();
            fcmToken.reassign(userId, deviceType);
            fcmTokenWriter.save(fcmToken);
            log.info("FCM 토큰 재할당: userId={}, deviceType={}", userId, deviceType);
        } else {
            fcmTokenWriter.save(FcmToken.create(userId, token, deviceType));
            log.info("FCM 토큰 등록: userId={}, deviceType={}", userId, deviceType);
        }
    }

    @Transactional
    public void unregister(String token) {
        fcmTokenWriter.deleteByToken(token);
        log.info("FCM 토큰 해제");
    }
}
