package org.example.shield.lawyer.application;

/**
 * 변호사 서비스 - 변호사 목록/프로필 조회/수정.
 *
 * Layer: application
 * Called by: LawyerController
 * Calls: LawyerProfileRepository, UserRepository
 *
 * TODO:
 * - getLawyers(pageable, specialization): 변호사 목록 (APPROVED만, 필터/페이지네이션)
 * - getLawyer(lawyerId): 변호사 프로필 상세 (의뢰인이 볼 때)
 * - getMyProfile(userId): 변호사 본인 프로필 (편집 화면 초기값)
 * - updateMyProfile(userId, request): 전문분야/경력/자격증 수정 (차후 구현)
 */
public class LawyerService {
}
