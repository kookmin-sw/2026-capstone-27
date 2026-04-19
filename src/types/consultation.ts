import type { ConsultationStatus, MessageRole } from './enums';

/** 명세: POST /api/consultations 요청 본문
 *  3단계 분류 체계 — domains(L1) / subDomains(L2) / tags(L3) 배열로 전달. */
export interface CreateConsultationRequest {
  domains: string[];
  subDomains: string[];
  tags: string[];
}

export interface CreateConsultationResponse {
  consultationId: string;
  status: string;
  welcomeMessage: string;
  createdAt: string;
}

/** 명세: GET /api/consultations 목록 및 상세 */
export interface ConsultationResponse {
  consultationId: string;
  status: ConsultationStatus;
  primaryField: string[] | null;
  tags: string[] | null;
  lastMessage: string | null;
  lastMessageAt: string | null;
  createdAt: string;
  brief: BriefSummary | null;
}

/** 명세: 상담에 연결된 의뢰서 요약 (nullable) */
export interface BriefSummary {
  briefId: string;
  title: string;
  status: string;
}

export interface MessageRequest {
  content: string;
}

/** 명세: GET /api/consultations/{id}/messages 개별 메시지 */
export interface MessageResponse {
  messageId: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}

/** 명세: POST /api/consultations/{id}/messages 전송 응답 (202) */
export interface SendMessageResponse {
  messageId: string;
  role: string;
  content: string;
  createdAt: string;
  allCompleted: boolean;
  classification?: {
    primaryField: string[];
    tags: string[];
  };
}
