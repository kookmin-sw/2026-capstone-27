package org.example.shield.consultation.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

import java.util.UUID;

public class ConsultationNotFoundException extends BusinessException {

    public ConsultationNotFoundException(UUID consultationId) {
        super(ErrorCode.CONSULTATION_NOT_FOUND,
                "상담을 찾을 수 없습니다. ID: " + consultationId);
    }
}
