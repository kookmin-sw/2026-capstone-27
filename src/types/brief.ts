import type { BriefStatus, PrivacySetting } from './enums';

export interface KeyIssue {
  title: string;
  description: string;
}

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

export interface BriefSummaryResponse {
  briefId: string;
  title: string;
  status: BriefStatus;
  createdAt: string;
}

export interface BriefUpdateRequest {
  title?: string;
  content?: string;
  keyIssues?: KeyIssue[];
  keywords?: string[];
  strategy?: string;
  privacySetting?: PrivacySetting;
}

export interface MatchingResponse {
  lawyerId: string;
  name: string;
  profileImageUrl: string;
  specializations: string;
  experienceYears: number;
  tags: string[];
  matchedKeywords: string[];
}

export interface DeliveryRequest {
  lawyerId: string;
}

export interface DeliveryResponse {
  deliveryId: string;
  lawyerId: string;
  lawyerName: string;
  status: string;
  sentAt: string;
}

export interface DeliveriesWrapper {
  deliveries: DeliveryResponse[];
}
