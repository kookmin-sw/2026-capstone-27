package org.example.shield.fcm.event;

import java.util.Map;
import java.util.UUID;

public record PushNotificationEvent(
        UUID userId,
        String title,
        String body,
        Map<String, String> data
) {
    public static PushNotificationEvent of(UUID userId, String title, String body) {
        return new PushNotificationEvent(userId, title, body, Map.of());
    }
}
