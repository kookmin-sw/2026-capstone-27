package org.example.shield.common.exception;

/**
 * 전역 예외 처리기 - 모든 예외를 ApiResponse.error()로 변환.
 *
 * TODO: @RestControllerAdvice
 * - BusinessException → 해당 ErrorCode의 HTTP 상태 + 메시지
 * - MethodArgumentNotValidException → 400 + 유효성 검사 실패 메시지
 * - Exception → 500 + "서버 내부 오류가 발생했습니다"
 */
public class GlobalExceptionHandler {
}
