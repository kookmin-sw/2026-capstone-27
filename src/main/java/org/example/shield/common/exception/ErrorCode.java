package org.example.shield.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 인증에 실패했습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

    // Lawyer
    LAWYER_NOT_FOUND(HttpStatus.NOT_FOUND, "변호사를 찾을 수 없습니다"),

    // Consultation
    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "상담을 찾을 수 없습니다"),
    CONSULTATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "대화가 완료되지 않았습니다"),

    // Brief
    BRIEF_NOT_FOUND(HttpStatus.NOT_FOUND, "의뢰서를 찾을 수 없습니다"),
    BRIEF_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 의뢰서입니다"),

    // Delivery
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 전달 건입니다"),

    // Verification
    VERIFICATION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 검증 신청된 상태입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
