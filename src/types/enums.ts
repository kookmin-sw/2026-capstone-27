export type ConsultationStatus =
  | 'COLLECTING'
  | 'ANALYZING'
  | 'AWAITING_CONFIRM'
  | 'CONFIRMED'
  | 'REJECTED';

export type BriefStatus = 'DRAFT' | 'CONFIRMED' | 'DELIVERED' | 'DISCARDED';

export type DomainType = 'CIVIL' | 'CRIMINAL' | 'LABOR' | 'SCHOOL_VIOLENCE';

export type MessageRole =
  | 'USER'
  | 'CHATBOT'
  | 'CHATBOT_TIP'
  | 'ROUTER_REQUEST'
  | 'SYSTEM';

export type DeliveryStatus = 'DELIVERED' | 'CONFIRMED' | 'REJECTED';

export type UserRole = 'USER' | 'LAWYER' | 'ADMIN';

export type VerificationStatus =
  | 'PENDING'
  | 'REVIEWING'
  | 'SUPPLEMENT_REQUESTED'
  | 'VERIFIED'
  | 'REJECTED';

export type PrivacySetting = 'PUBLIC' | 'PARTIAL';

export type OAuthProvider = 'kakao' | 'naver' | 'google';
