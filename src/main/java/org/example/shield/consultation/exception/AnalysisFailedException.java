package org.example.shield.consultation.exception;

import org.example.shield.common.exception.BusinessException;
import org.example.shield.common.exception.ErrorCode;

/**
 * AI 분석 실패 예외.
 * Groq API 호출 실패, 타임아웃, JSON 파싱 실패 시 발생.
 */
public class AnalysisFailedException extends BusinessException {

    public AnalysisFailedException(String detail) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, detail);
    }

    public AnalysisFailedException(String detail, Throwable cause) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, detail, cause);
    }
}
