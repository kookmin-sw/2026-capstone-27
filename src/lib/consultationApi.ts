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

const BASE = '/consultations';

export const consultationApi = {
  /** 내 상담 목록 */
  getList: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<ConsultationResponse>>>(BASE, {
      params: { page, size },
    }),

  /** 상담 상세 */
  getById: (id: string) =>
    api.get<ApiResponse<ConsultationResponse>>(`${BASE}/${id}`),

  /** 새 상담 생성 */
  create: (request: CreateConsultationRequest) =>
    api.post<ApiResponse<CreateConsultationResponse>>(BASE, request),

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

  /** 분류 수정 — BE ClassifyRequest/ClassifyResponse 와 정합 맞춤.
   *  3단계 분류 체계 — domains(L1) / subDomains(L2) / tags(L3) 을 독립 배열로 전달. */
  updateClassify: (
    id: string,
    data: { domains: string[]; subDomains: string[]; tags: string[] },
  ) =>
    api.patch<
      ApiResponse<{ domains: string[]; subDomains: string[]; tags: string[] }>
    >(`${BASE}/${id}/classify`, data),

  /** 의뢰서 생성 요청 (비동기) */
  requestAnalyze: (id: string) =>
    api.post<ApiResponse<void>>(`${BASE}/${id}/analyze`),
};
