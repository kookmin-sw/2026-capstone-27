package org.example.shield.lawyer.controller;

/**
 * 변호사 API 컨트롤러.
 *
 * Layer: controller
 * Called by: 프론트엔드
 * Calls: LawyerService, VerificationService
 *
 * API 목록 (6개):
 * - GET   /api/lawyers                             변호사 목록 조회
 * - GET   /api/lawyers/{lawyerId}                  변호사 프로필 상세
 * - GET   /api/lawyers/me                          내 프로필 조회
 * - PATCH /api/lawyers/me                          내 프로필 수정 (차후 구현)
 * - POST  /api/lawyers/me/verification-request     검증 신청 (body: barAssociationNumber)
 * - GET   /api/lawyers/me/verification-status      검증 상태 확인
 */
public class LawyerController {
}
