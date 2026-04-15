import type { VerificationStatus } from './enums';

export interface LawyerResponse {
  lawyerId: string;
  name: string;
  profileImageUrl?: string;
  specializations: string[];
  experienceYears: number;
  introduction?: string;
  verificationStatus: VerificationStatus;
  rating?: number;
  reviewCount?: number;
}

export interface LawyerDetailResponse extends LawyerResponse {
  email: string;
  phone?: string;
  licenseNumber: string;
  officeAddress?: string;
}

export interface InboxItemResponse {
  deliveryId: string;
  briefId: string;
  title: string;
  legalField: string;
  status: string;
  createdAt: string;
}

export interface InboxStatsResponse {
  total: number;
  pending: number;
  accepted: number;
  rejected: number;
}
