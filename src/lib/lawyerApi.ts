import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type {
  LawyerResponse,
  LawyerDetailResponse,
  LawyerMeResponse,
} from '@/types/lawyer';

const BASE = '/lawyers';

export const lawyerApi = {
  /** 변호사 목록 (의뢰인용) */
  getList: (page = 0, size = 20, specialization?: string, minExperience?: number, sort?: string) =>
    api.get<ApiResponse<PageResponse<LawyerResponse>>>(BASE, {
      params: { page, size, specialization, minExperience, sort },
    }),

  /** 변호사 프로필 상세 */
  getById: (id: string) =>
    api.get<ApiResponse<LawyerDetailResponse>>(`${BASE}/${id}`),

  /** 내 프로필 (변호사) */
  getMe: () =>
    api.get<ApiResponse<LawyerMeResponse>>(`${BASE}/me`),

  /** 프로필 수정 (변호사) */
  updateMe: (data: Partial<LawyerMeResponse>) =>
    api.patch<ApiResponse<LawyerMeResponse>>(`${BASE}/me`, data),
};
