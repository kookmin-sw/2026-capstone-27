import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { LawyerDetailResponse } from '@/types/lawyer';

const BASE = '/admin';

export interface AdminStats {
  totalUsers: number;
  totalLawyers: number;
  pendingVerifications: number;
  totalConsultations: number;
}

export interface AdminAlert {
  id: string;
  type: string;
  message: string;
  createdAt: string;
}

export interface VerificationCheck {
  checkType: string;
  result: string;
  detail: string;
}

export const adminApi = {
  /** 대시보드 통계 */
  getStats: () =>
    api.get<ApiResponse<AdminStats>>(`${BASE}/dashboard/stats`),

  /** 긴급 알림 */
  getAlerts: () =>
    api.get<ApiResponse<AdminAlert[]>>(`${BASE}/dashboard/alerts`),

  /** 심사 대기 변호사 목록 */
  getPendingLawyers: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<LawyerDetailResponse>>>(
      `${BASE}/lawyers/pending`,
      { params: { page, size } },
    ),

  /** 변호사 상세 */
  getLawyerDetail: (id: string) =>
    api.get<ApiResponse<LawyerDetailResponse>>(`${BASE}/lawyers/${id}`),

  /** 심사 처리 */
  processVerification: (
    id: string,
    data: { status: string; reason?: string },
  ) =>
    api.patch<ApiResponse<void>>(`${BASE}/lawyers/${id}/verification`, data),

  /** 자동 검증 결과 */
  getVerificationChecks: (id: string) =>
    api.get<ApiResponse<VerificationCheck[]>>(
      `${BASE}/lawyers/${id}/verification-checks`,
    ),

  /** 서류 조회 */
  getDocuments: (id: string) =>
    api.get<ApiResponse<Array<{ id: string; fileName: string; fileUrl: string; uploadedAt: string }>>>(
      `${BASE}/lawyers/${id}/documents`,
    ),
};
