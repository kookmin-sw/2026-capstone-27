import type { VerificationStatus } from './enums';

/** GET /api/admin/dashboard/stats — BE DashboardStatsResponse 와 1:1 */
export interface AdminStats {
  pendingCount: number;
  reviewingCount: number;
  supplementRequestedCount: number;
  todayProcessedCount: number;
}

/** GET /api/admin/dashboard/alerts — BE DashboardAlertsResponse 와 1:1 */
export interface AdminAlerts {
  overdueCount: number;
  missingDocumentCount: number;
  duplicateSuspectCount: number;
}

/**
 * GET /api/admin/lawyers/pending 목록 아이템 — BE PendingLawyerResponse 와 1:1.
 * 공용 LawyerResponse 와 달리 tags/certifications/caseCount/bio/region 은 없고
 * email/phone/documentCount/createdAt 를 포함한다.
 */
export interface PendingLawyerResponse {
  lawyerId: string;
  name: string;
  email: string;
  phone: string;
  domains: string[];
  subDomains: string[];
  experienceYears: number;
  verificationStatus: VerificationStatus;
  documentCount: number;
  createdAt: string;
}

/**
 * GET /api/admin/lawyers/{lawyerId} — BE admin.LawyerDetailResponse 와 1:1.
 * 공용 LawyerResponse 와 달리 userId/email/phone/barAssociationNumber/createdAt
 * 이 추가되고 profileImageUrl 은 없다.
 */
export interface AdminLawyerDetailResponse {
  lawyerId: string;
  userId: string;
  name: string;
  email: string;
  phone: string;
  domains: string[];
  subDomains: string[];
  experienceYears: number;
  certifications: string[];
  barAssociationNumber: string;
  verificationStatus: VerificationStatus;
  region: string;
  bio: string;
  caseCount: number;
  tags: string[];
  createdAt: string;
}

/** GET /api/admin/lawyers/{lawyerId}/verification-checks — BE VerificationChecksResponse 와 1:1 */
export interface VerificationChecks {
  lawyerId: string;
  emailDuplicate: boolean;
  phoneDuplicate: boolean;
  nameDuplicate: boolean;
  requiredFields: boolean;
  licenseVerified: boolean;
  documentMatched: boolean;
  specializationValid: boolean;
  experienceVerified: boolean;
  duplicateSignup: boolean;
  documentComplete: boolean;
  completedCount: number;
  totalCount: number;
  updatedAt: string | null;
}

/**
 * GET /api/admin/verification-logs 목록 아이템 — BE VerificationLogResponse 와 1:1.
 * 상태 전이는 fromStatus → toStatus 로 기록되며, 담당자는 adminName 으로 내려온다.
 */
export interface VerificationLog {
  logId: string;
  lawyerName: string;
  fromStatus: string;
  toStatus: string;
  domains: string[];
  adminName: string;
  reason: string | null;
  createdAt: string;
}

/**
 * PATCH /api/admin/lawyers/{lawyerId}/verification 요청
 * BE VerificationRequest 와 1:1. status 는 필수, reason 은 승인(APPROVED) 제외 필수.
 */
export interface VerificationActionRequest {
  status: string;
  reason?: string;
}

/** PATCH /api/admin/lawyers/{lawyerId}/verification 응답 — BE VerificationResponse 와 1:1 */
export interface VerificationActionResponse {
  lawyerId: string;
  previousStatus: string;
  newStatus: string;
  reason: string | null;
  processedAt: string;
}

/**
 * GET /api/admin/verification-logs 쿼리 파라미터.
 * BE 는 period (today/week) 와 status 를 따로 받는다. FE 에서 날짜 범위는 쓰지 않는다.
 */
export interface VerificationLogsFilter {
  period?: 'today' | 'week';
  status?: string;
}
