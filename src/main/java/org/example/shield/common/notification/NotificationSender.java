package org.example.shield.common.notification;

public interface NotificationSender {

    void send(String to, String subject, String content);
}
