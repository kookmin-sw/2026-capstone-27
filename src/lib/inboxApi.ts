import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { InboxItemResponse, InboxStatsResponse } from '@/types/lawyer';
import type { BriefResponse } from '@/types/brief';

const BASE = '/lawyer/inbox';

export const inboxApi = {
  /** 수신 의뢰서 목록 */
  getList: (page = 0, size = 20, status?: string) =>
    api.get<ApiResponse<PageResponse<InboxItemResponse>>>(BASE, {
      params: { page, size, status },
    }),

  /** 수신함 통계 */
  getStats: () =>
    api.get<ApiResponse<InboxStatsResponse>>(`${BASE}/stats`),

  /** 수신 의뢰서 상세 */
  getById: (id: string) =>
    api.get<ApiResponse<BriefResponse>>(`${BASE}/${id}`),

  /** 수락/거절 */
  updateStatus: (id: string, status: 'CONFIRMED' | 'REJECTED') =>
    api.patch<ApiResponse<void>>(`${BASE}/${id}/status`, { status }),
};
