package org.example.shield.common.enums;

/**
 * 의뢰서 상태 ENUM.
 *
 * DRAFT     - 초안 (AI 생성 직후)
 * CONFIRMED - 사용자 확정
 * DELIVERED - 변호사에게 전달
 * REJECTED  - 거절
 */
public enum BriefStatus {
    DRAFT,
    CONFIRMED,
    DELIVERED,
    REJECTED
}
