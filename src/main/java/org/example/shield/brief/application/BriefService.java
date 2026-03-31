package org.example.shield.brief.application;

/**
 * 의뢰서 서비스 - 의뢰서 CRUD + 상태 변경.
 *
 * Layer: application
 * Called by: BriefController
 * Calls: BriefRepository
 *
 * TODO:
 * - getMyBriefs(userId, pageable): 의뢰인의 의뢰서 목록
 * - getBrief(briefId): 의뢰서 상세 (content 줄글 + keywords + status)
 * - updateBrief(briefId, request):
 *   - status가 CONFIRMED이면 수정 차단 (BriefAlreadyConfirmedException)
 *   - body에 status: "CONFIRMED" → 필수항목 검증 후 확정
 *   - body에 status: "DISCARDED" → 폐기
 *   - body에 content → 줄글 수정
 *   - body에 privacySetting → 개인정보 설정 변경
 */
public class BriefService {
}
