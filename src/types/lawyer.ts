import type { VerificationStatus } from './enums';
import type { KeyIssue } from './brief';

/** 명세: GET /api/lawyers 목록 아이템 */
export interface LawyerResponse {
  lawyerId: string;
  name: string;
  profileImageUrl?: string;
  specializations: string;
  experienceYears: number;
  tags: string[];
  bio: string;
  region: string;
  verificationStatus: VerificationStatus;
}

/** 명세: GET /api/lawyers/{lawyerId} 상세 */
export interface LawyerDetailResponse extends LawyerResponse {
  certifications: string[];
  caseCount: number;
}

/** 명세: GET /api/lawyers/me 내 프로필 */
export interface LawyerMeResponse {
  lawyerId: string;
  name: string;
  profileImageUrl?: string;
  specializations: string;
  experienceYears: number;
  tags: string[];
  certifications: string[];
  caseCount: number;
  bio: string;
  region: string;
  verificationStatus: VerificationStatus;
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
