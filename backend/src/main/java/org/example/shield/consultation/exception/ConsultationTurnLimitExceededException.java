package org.example.shield.consultation.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

import java.util.UUID;

/**
 * 사용자 메시지 턴 상한(기본 10)을 넘겨 sendMessage 를 다시 호출한 경우.
 *
 * <p>정상 FE 는 {@code SendMessageResponse.allCompleted=true} 신호에서 멈추고
 * {@code /analyze} 로 전환하므로 이 예외는 방어선이다.</p>
 */
public class ConsultationTurnLimitExceededException extends BusinessException {

    public ConsultationTurnLimitExceededException(UUID consultationId, int maxUserTurns) {
        super(ErrorCode.CONSULTATION_TURN_LIMIT_EXCEEDED,
                "상담 대화 횟수 상한(" + maxUserTurns + ")을 초과했습니다. consultationId=" + consultationId);
    }
}
