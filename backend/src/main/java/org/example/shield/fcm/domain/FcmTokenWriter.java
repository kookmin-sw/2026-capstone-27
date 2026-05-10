package org.example.shield.fcm.domain;

public interface FcmTokenWriter {

    FcmToken save(FcmToken token);

    void deleteByToken(String token);
}
