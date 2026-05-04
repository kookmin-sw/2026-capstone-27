package org.example.shield.fcm.application;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.brief.application.DeliveryStatusEvent;
import org.example.shield.fcm.domain.FcmToken;
import org.example.shield.fcm.domain.FcmTokenReader;
import org.example.shield.fcm.domain.FcmTokenWriter;
import org.example.shield.fcm.event.PushNotificationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationSender {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenReader fcmTokenReader;
    private final FcmTokenWriter fcmTokenWriter;

    @Async("fcmExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPushEvent(PushNotificationEvent event) {
        try {
            sendToUser(event.userId(), event.title(), event.body(), event.data());
        } catch (Exception e) {
            log.error("FCM 발송 중 예외: userId={}, title={}", event.userId(), event.title(), e);
        }
    }

    @Async("fcmExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryStatusChanged(DeliveryStatusEvent event) {
        try {
            String title;
            String body;
            if ("CONFIRMED".equals(event.status())) {
                title = "변호사 매칭 완료";
                body = event.lawyerName() + " 변호사가 \"" + event.briefTitle() + "\" 의뢰서를 수락했습니다";
            } else {
                title = "의뢰서 거절됨";
                body = event.lawyerName() + " 변호사가 의뢰서를 거절했습니다";
            }
            Map<String, String> data = Map.of(
                    "type", "DELIVERY_STATUS",
                    "status", event.status()
            );
            sendToUser(event.clientUserId(), title, body, data);
        } catch (Exception e) {
            log.error("의뢰서 상태 변경 푸시 발송 중 예외: clientUserId={}, status={}",
                    event.clientUserId(), event.status(), e);
        }
    }

    private void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = fcmTokenReader.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("FCM 토큰 없음, 발송 스킵: userId={}", userId);
            return;
        }

        List<String> tokenStrings = tokens.stream().map(FcmToken::getToken).toList();

        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokenStrings)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(builder.build());
            log.info("FCM 발송 완료: userId={}, success={}, failure={}",
                    userId, response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                cleanupInvalidTokens(response.getResponses(), tokenStrings);
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패: userId={}, code={}", userId, e.getMessagingErrorCode(), e);
        }
    }

    private void cleanupInvalidTokens(List<SendResponse> responses, List<String> tokens) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) continue;

            FirebaseMessagingException ex = r.getException();
            if (ex == null) continue;

            MessagingErrorCode code = ex.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                String invalidToken = tokens.get(i);
                fcmTokenWriter.deleteByToken(invalidToken);
                log.info("무효 FCM 토큰 삭제: code={}", code);
            }
        }
    }
}
