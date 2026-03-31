package org.example.shield.brief.controller;

/**
 * 변호사 수신함 API 컨트롤러.
 *
 * Layer: controller
 * Called by: 프론트엔드 (변호사 화면)
 * Calls: DeliveryService
 *
 * API 목록 (3개):
 * - GET   /api/lawyer/inbox                        수신 의뢰서 목록
 * - GET   /api/lawyer/inbox/{deliveryId}            수신 의뢰서 상세
 * - PATCH /api/lawyer/inbox/{deliveryId}/status     접수/거절 (body: status, rejectionReason)
 */
public class LawyerInboxController {
}
