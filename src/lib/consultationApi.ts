import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type {
  CreateConsultationRequest,
  CreateConsultationResponse,
  ConsultationResponse,
  MessageRequest,
  MessageResponse,
  SendMessageResponse,
} from '@/types/consultation';
import type { DomainType } from '@/types/enums';

const BASE = '/consultations';

export const consultationApi = {
  /** 내 상담 목록 */
  getList: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<ConsultationResponse>>>(BASE, {
      params: { page, size },
    }),

  /** 상담 상세 */
  getById: (id: string) =>
    api.post<ApiResponse<ConsultationResponse>>(`${BASE}/${id}`),

  /** 새 상담 생성 */
  create: (domain: DomainType | null) =>
    api.post<ApiResponse<CreateConsultationResponse>>(BASE, {
      domain,
    } satisfies CreateConsultationRequest),

  /** 메시지 목록 */
  getMessages: (id: string, page = 0, size = 50) =>
    api.get<ApiResponse<PageResponse<MessageResponse>>>(`${BASE}/${id}/messages`, {
      params: { page, size },
    }),

  /** 메시지 전송 */
  sendMessage: (id: string, content: string) =>
    api.post<ApiResponse<SendMessageResponse>>(`${BASE}/${id}/messages`, {
      content,
    } satisfies MessageRequest),

  /** 분류 수정 */
  updateClassify: (
    id: string,
    data: { primaryField: string[]; tags: string[] },
  ) => api.patch<ApiResponse<void>>(`${BASE}/${id}/classify`, data),

  /** 의뢰서 생성 요청 (비동기) */
  requestAnalyze: (id: string) =>
    api.post<ApiResponse<void>>(`${BASE}/${id}/analyze`),

  /** 법률 분야 목록 (BE에서 동적 조회) */
  getLegalFields: () =>
    api.get<ApiResponse<string[]>>(`${BASE}/legal-fields`),
};
