package org.example.shield.lawyer.application;

/**
 * 변호사 검증 서비스 - 검증 신청/상태 확인/관리자 처리.
 *
 * Layer: application
 * Called by: LawyerController (신청/상태), AdminController (처리)
 * Calls: LawyerProfileRepository
 *
 * TODO:
 * - requestVerification(userId, barAssociationNumber):
 *   1. lawyer_profiles에 barAssociationNumber 저장
 *   2. verificationStatus를 PENDING으로 설정
 *   3. 이미 PENDING이면 409 에러
 *
 * - getVerificationStatus(userId): verificationStatus + verifiedAt 반환
 *
 * - processVerification(lawyerId, action, reason):
 *   1. action이 APPROVE면 → APPROVED + verifiedAt 설정
 *   2. action이 REJECT면 → REJECTED
 *   3. 관리자(ADMIN)만 호출 가능
 */
public class VerificationService {
}
