package org.example.shield.common.enums;

/**
 * 변호사 검증 상태 ENUM.
 *
 * PENDING              - 승인 대기
 * REVIEWING            - 검토 중
 * SUPPLEMENT_REQUESTED - 보완 요청
 * VERIFIED             - 승인 완료
 * REJECTED             - 거절
 */
public enum VerificationStatus {
    PENDING,
    REVIEWING,
    SUPPLEMENT_REQUESTED,
    VERIFIED,
    REJECTED
}
