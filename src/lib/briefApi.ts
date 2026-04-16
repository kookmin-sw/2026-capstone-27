import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type {
  BriefResponse,
  BriefSummaryResponse,
  BriefUpdateRequest,
  MatchingResponse,
  DeliveryRequest,
  DeliveryResponse,
  DeliveriesWrapper,
} from '@/types/brief';

const BASE = '/briefs';

export const briefApi = {
  /** 내 의뢰서 목록 */
  getList: (page = 0, size = 20, status?: string) =>
    api.get<ApiResponse<PageResponse<BriefSummaryResponse>>>(BASE, {
      params: { page, size, status },
    }),

  /** 의뢰서 상세 */
  getById: (id: string) =>
    api.get<ApiResponse<BriefResponse>>(`${BASE}/${id}`),

  /** 의뢰서 수정 */
  update: (id: string, data: BriefUpdateRequest) =>
    api.patch<ApiResponse<BriefResponse>>(`${BASE}/${id}`, data),

  /** 의뢰서 확정 */
  confirm: (id: string) =>
    api.patch<ApiResponse<BriefResponse>>(`${BASE}/${id}`, {
      status: 'CONFIRMED',
    }),

  /** 변호사 추천 (키워드 매칭) */
  getRecommendations: (id: string) =>
    api.get<ApiResponse<MatchingResponse[]>>(
      `${BASE}/${id}/lawyer-recommendations`,
    ),

  /** 의뢰서 전달 */
  deliver: (id: string, lawyerId: string) =>
    api.post<ApiResponse<DeliveryResponse>>(`${BASE}/${id}/deliveries`, {
      lawyerId,
    } satisfies DeliveryRequest),

  /** 전달 현황 */
  getDeliveries: (id: string) =>
    api.get<ApiResponse<DeliveriesWrapper>>(`${BASE}/${id}/deliveries`),
};
