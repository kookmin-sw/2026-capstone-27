package org.example.shield.brief.application;

/**
 * 변호사 매칭 서비스 - 의뢰서 기반으로 최적의 변호사 1명 선정.
 *
 * Layer: application
 * Called by: BriefController.getLawyerRecommendation()
 * Calls: BriefRepository, LawyerProfileRepository
 *
 * TODO:
 * - findMatch(briefId):
 *   1. brief에서 keywords, legalField 조회
 *   2. lawyer_profiles에서 verificationStatus = APPROVED인 변호사만 필터
 *   3. specializations에 legalField가 포함된 변호사 필터
 *   4. 매칭 결과 없으면 404
 */
public class LawyerMatchingService {
}
