package org.example.shield.brief.application;

/**
 * 의뢰서 전달 서비스 - 전달/현황조회/접수/거절.
 *
 * Layer: application
 * Called by: BriefController, LawyerInboxController
 * Calls: BriefDeliveryRepository, BriefRepository
 *
 * TODO:
 * - createDelivery(briefId, lawyerId):
 *   1. brief가 CONFIRMED 상태인지 확인
 *   2. brief_deliveries에 새 row (status: SENT, sentAt: now())
 *
 * - getDeliveries(briefId): 전달 현황 (의뢰인용)
 * - getInbox(lawyerId, pageable): 수신 의뢰서 목록 (변호사용)
 * - getInboxDetail(deliveryId): 수신 의뢰서 상세 (privacySetting 적용)
 * - updateDeliveryStatus(deliveryId, status, rejectionReason):
 *   - ACCEPTED: 접수 → respondedAt 기록
 *   - REJECTED: 거절 → rejectionReason 저장 + respondedAt 기록
 */
public class DeliveryService {
}
