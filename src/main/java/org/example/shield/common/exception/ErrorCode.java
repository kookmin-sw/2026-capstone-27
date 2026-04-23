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

    // Auth - Token
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다"),

    // Auth - OAuth
    OAUTH_CODE_INVALID(HttpStatus.UNAUTHORIZED, "OAuth 인증 코드가 유효하지 않습니다"),
    OAUTH_USER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 사용자 정보 조회에 실패했습니다"),

    // Auth - Role
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 역할입니다 (USER, LAWYER, ADMIN)"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

    // Lawyer
    LAWYER_NOT_FOUND(HttpStatus.NOT_FOUND, "변호사를 찾을 수 없습니다"),
    LAWYER_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "인증되지 않은 변호사에게는 전달할 수 없습니다"),

    // Consultation
    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "상담을 찾을 수 없습니다"),
    CONSULTATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "대화가 완료되지 않았습니다"),
    CONSULTATION_TURN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "상담 대화 횟수 상한을 초과했습니다. 의뢰서 생성으로 넘어가 주세요"),

    // Brief
    BRIEF_NOT_FOUND(HttpStatus.NOT_FOUND, "의뢰서를 찾을 수 없습니다"),
    BRIEF_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 의뢰서입니다"),

    // Delivery
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 전달 건입니다"),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 전달된 변호사입니다"),
    DELIVERY_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 의뢰서입니다"),

    // Brief - Delivery
    BRIEF_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "의뢰서를 먼저 확정해 주세요"),

    // Verification
    VERIFICATION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 검증 신청된 상태입니다"),

    // Admin
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다"),
    VERIFICATION_CONFLICT(HttpStatus.CONFLICT, "다른 관리자가 이미 처리한 건입니다. 새로고침 후 다시 시도해주세요"),
    VERIFICATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 최종 처리된 변호사입니다"),

    // Document
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다"),
    DOCUMENT_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 10MB를 초과합니다"),
    DOCUMENT_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다"),

    // AI
    CHAT_AI_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 생성에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
