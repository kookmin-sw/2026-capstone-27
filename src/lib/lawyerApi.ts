import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type {
  LawyerResponse,
  LawyerDetailResponse,
} from '@/types/lawyer';

const BASE = '/lawyers';

export const lawyerApi = {
  /** 변호사 목록 (의뢰인용) */
  getList: (page = 0, size = 20, specialization?: string) =>
    api.get<ApiResponse<PageResponse<LawyerResponse>>>(BASE, {
      params: { page, size, specialization },
    }),

  /** 변호사 프로필 상세 */
  getById: (id: string) =>
    api.get<ApiResponse<LawyerDetailResponse>>(`${BASE}/${id}`),

  /** 내 프로필 (변호사) */
  getMe: () =>
    api.get<ApiResponse<LawyerDetailResponse>>(`${BASE}/me`),

  /** 프로필 수정 (변호사) */
  updateMe: (data: Partial<LawyerDetailResponse>) =>
    api.patch<ApiResponse<LawyerDetailResponse>>(`${BASE}/me`, data),
};
