package org.example.shield.common.enums;

/**
 * 상담 상태 ENUM.
 *
 * COLLECTING       - 대화 중 (정보 수집)
 * ANALYZING        - 의뢰서 생성 중 (비동기)
 * AWAITING_CONFIRM - 의뢰서 확인 대기
 * CONFIRMED        - 사용자 확정 완료
 * REJECTED         - 취소/실패
 */
public enum ConsultationStatus {
    COLLECTING,
    ANALYZING,
    AWAITING_CONFIRM,
    CONFIRMED,
    REJECTED
}
