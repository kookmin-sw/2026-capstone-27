import type { BriefStatus, PrivacySetting } from './enums';

/** 명세: 쟁점 구조 */
export interface KeyIssue {
  title: string;
  description: string;
}

/** 명세: GET /api/briefs/{id} 의뢰서 상세 */
export interface BriefResponse {
  briefId: string;
  title: string;
  legalField: string;
  content: string;
  keyIssues: KeyIssue[];
  keywords: string[];
  strategy: string;
  privacySetting: PrivacySetting;
  status: BriefStatus;
  createdAt: string;
}

/** 명세: GET /api/briefs 목록 내 요약 */
export interface BriefSummaryResponse {
  briefId: string;
  title: string;
  status: BriefStatus;
  createdAt: string;
}

/** 명세: PATCH /api/briefs/{id} 수정 요청 */
export interface BriefUpdateRequest {
  title?: string;
  content?: string;
  keyIssues?: KeyIssue[];
  keywords?: string[];
  strategy?: string;
  privacySetting?: PrivacySetting;
  status?: string;
}

/** 명세: GET /api/briefs/{id}/lawyer-recommendations */
export interface MatchingResponse {
  lawyerId: string;
  name: string;
  profileImageUrl: string;
  specializations: string;
  experienceYears: number;
  tags: string[];
  bio: string;
  region: string;
  score: number;
  matchedKeywords?: string[];
}

export interface DeliveryRequest {
  lawyerId: string;
}

/** 명세: deliveries 응답 — sentAt 사용, viewedAt/respondedAt nullable */
export interface DeliveryResponse {
  deliveryId: string;
  lawyerName: string;
  status: string;
  sentAt: string;
  viewedAt: string | null;
  respondedAt: string | null;
}

/** 명세: GET /api/briefs/{id}/deliveries 래핑 응답 */
export interface DeliveriesWrapper {
  deliveries: DeliveryResponse[];
}
