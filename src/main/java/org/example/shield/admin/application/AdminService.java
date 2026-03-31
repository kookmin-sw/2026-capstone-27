package org.example.shield.admin.application;

/**
 * 관리자 서비스 - 변호사 검증 + 상담 모니터링.
 *
 * Layer: application
 * Called by: AdminController
 * Calls: LawyerProfileRepository, ConsultationRepository, VerificationService
 *
 * TODO:
 * - getPendingLawyers(pageable): verificationStatus = PENDING인 변호사 목록
 * - processVerification(lawyerId, action, reason): VerificationService 위임
 * - getConsultations(pageable, status): 전체 상담 현황 (차후 구현)
 */
public class AdminService {
}
