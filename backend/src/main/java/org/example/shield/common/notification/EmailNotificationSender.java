package org.example.shield.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shield.brief.application.DeliveryStatusEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("이메일 발송 완료. to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("이메일 발송 실패. to={}, subject={}", to, subject, e);
        }
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryStatusChanged(DeliveryStatusEvent event) {
        log.info("의뢰서 상태 변경 이메일 발송. clientEmail={}, status={}", event.clientEmail(), event.status());

        String subject;
        String body;

        if ("CONFIRMED".equals(event.status())) {
            subject = "[SHIELD] " + event.lawyerName() + " 변호사가 의뢰서를 수락하였습니다";
            body = event.clientName() + "님, 안녕하세요.\n\n"
                    + event.lawyerName() + " 변호사가 \"" + event.briefTitle() + "\" 의뢰서를 수락하였습니다.\n\n"
                    + "SHIELD에서 자세한 내용을 확인해 주세요.";
        } else {
            subject = "[SHIELD] " + event.lawyerName() + " 변호사가 의뢰서를 거절하였습니다";
            body = event.clientName() + "님, 안녕하세요.\n\n"
                    + event.lawyerName() + " 변호사가 \"" + event.briefTitle() + "\" 의뢰서를 거절하였습니다.\n"
                    + "사유: " + event.rejectionReason() + "\n\n"
                    + "다른 변호사에게 의뢰서를 전달해 보세요.";
        }

        send(event.clientEmail(), subject, body);
    }
}
