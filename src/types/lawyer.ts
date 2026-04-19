import type { VerificationStatus } from './enums';
import type { KeyIssue } from './brief';

/** 명세: GET /api/lawyers 목록 아이템 + GET /api/lawyers/{lawyerId} 상세
 *  BE LawyerResponse 와 정합 맞춤. 전문분야는 3단계(domains/subDomains/tags)로
 *  분리되며, 목록/상세/me 세 엔드포인트 모두 동일 DTO를 내려준다. */
export interface LawyerResponse {
  lawyerId: string;
  name: string;
  profileImageUrl: string | null;
  /** L1 대분류 */
  domains: string[];
  /** L2 중분류 */
  subDomains: string[];
  experienceYears: number;
  /** L3 키워드 / 태그 */
  tags: string[];
  certifications: string[];
  caseCount: number;
  bio: string;
  region: string;
  verificationStatus: VerificationStatus;
}

/** GET /api/lawyers/{lawyerId} 동일 DTO (별칭 유지) */
export type LawyerDetailResponse = LawyerResponse;

/** GET /api/lawyers/me 동일 DTO (별칭 유지) */
export type LawyerMeResponse = LawyerResponse;

/** PATCH /api/lawyers/me 요청 — BE ProfileUpdateRequest 와 1:1 */
export interface ProfileUpdateRequest {
  domains: string[];
  subDomains: string[];
  experienceYears: number;
  certifications: string[];
  tags: string[];
  bio: string;
  region: string;
}

/** POST /api/lawyers/me/register 요청 — BE LawyerRegisterRequest 와 1:1 */
export interface LawyerRegisterRequest {
  barAssociationNumber: string;
  domains: string[];
  subDomains: string[];
  tags: string[];
  experienceYears: number;
  bio: string;
  region: string;
}

export interface InboxItemResponse {
  deliveryId: string;
  briefId: string;
  briefTitle: string;
  legalField: string;
  status: string;
  sentAt: string;
}

export interface InboxStatsResponse {
  total: number;
  pending: number;
  confirmed: number;
  rejected: number;
}

export interface InboxDetailResponse {
  deliveryId: string;
  briefId: string;
  title: string;
  legalField: string;
  content: string;
  keywords: string[];
  keyIssues: KeyIssue[];
  status: string;
  clientName: string;
  clientEmail: string;
  sentAt: string;
}

/** GET /api/lawyers/me/verification-status 응답 */
export interface VerificationStatusResponse {
  status: VerificationStatus;
  requestedAt?: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

/** POST /api/lawyers/me/verification-request 요청 */
export interface VerificationRequestData {
  barAssociationNumber: string;
}

/**
 * GET /api/lawyers/me/documents 아이템 — BE DocumentResponse 와 1:1.
 * 주의: BE 는 uploadedAt 이 아닌 createdAt 으로 내려주며, fileSize(Long) 가 포함된다.
 */
export interface DocumentResponse {
  documentId: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  createdAt: string;
}
