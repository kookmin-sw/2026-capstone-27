package org.example.shield.common.exception;

/**
 * 에러 코드 Enum.
 *
 * TODO:
 * - httpStatus: HttpStatus
 * - message: String
 *
 * 값:
 * - INVALID_INPUT_VALUE(400, "잘못된 입력값입니다")
 * - UNAUTHORIZED(401, "인증이 필요합니다")
 * - ACCESS_DENIED(403, "접근 권한이 없습니다")
 * - USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다")
 * - CONSULTATION_NOT_FOUND(404, "상담을 찾을 수 없습니다")
 * - CONSULTATION_NOT_CLASSIFIED(400, "분류를 먼저 진행해주세요")
 * - BRIEF_NOT_FOUND(404, "의뢰서를 찾을 수 없습니다")
 * - BRIEF_ALREADY_CONFIRMED(409, "이미 확정된 의뢰서입니다")
 * - LAWYER_NOT_FOUND(404, "변호사를 찾을 수 없습니다")
 */
public enum ErrorCode {
}
