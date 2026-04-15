import type { ConsultationStatus, DomainType, MessageRole } from './enums';

export interface CreateConsultationRequest {
  domain: DomainType | null;
}

export interface CreateConsultationResponse {
  consultationId: string;
  status: string;
  welcomeMessage: string;
  createdAt: string;
}

export interface ConsultationResponse {
  consultationId: string;
  status: ConsultationStatus;
  primaryField: string[] | null;
  tags: string[] | null;
  lastMessage: string | null;
  lastMessageAt: string | null;
  createdAt: string;
}

export interface MessageRequest {
  content: string;
}

export interface MessageResponse {
  sender: MessageRole;
  content: string;
  timestamp: string;
}

export interface SendMessageResponse {
  content: string;
  timestamp: string;
  allCompleted: boolean;
  classification?: {
    primaryField: string[];
    tags: string[];
  };
}
