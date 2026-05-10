package org.example.shield.common.notification;

import java.util.UUID;

public interface NotificationSender {

    void send(String to, String subject, String content);

    /**
     * 의뢰서 생성 완료 알림.
     */
    default void sendBriefReadyNotification(UUID userId, UUID briefId) {
        // 기본 구현: 서브클래스에서 오버라이드
    }

    /**
     * 의뢰서 생성 실패 알림.
     */
    default void sendBriefFailedNotification(UUID userId) {
        // 기본 구현: 서브클래스에서 오버라이드
    }
}
