package org.example.shield.brief.controller;

/**
 * 의뢰서 API 컨트롤러.
 *
 * Layer: controller
 * Called by: 프론트엔드
 * Calls: BriefService, DeliveryService, LawyerMatchingService
 *
 * API 목록 (6개):
 * - GET   /api/briefs                                   내 의뢰서 목록
 * - GET   /api/briefs/{briefId}                         의뢰서 조회
 * - PATCH /api/briefs/{briefId}                         의뢰서 수정 (내용+상태+개인정보)
 * - GET   /api/briefs/{briefId}/lawyer-recommendations  변호사 매칭 조회 (1명)
 * - POST  /api/briefs/{briefId}/deliveries              의뢰서 전달 (1명)
 * - GET   /api/briefs/{briefId}/deliveries              전달 현황 조회
 */
public class BriefController {
}
