import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { LawyerDetailResponse } from '@/types/lawyer';

const BASE = '/admin';

export interface AdminStats {
  pendingCount: number;
  reviewingCount: number;
  supplementRequestedCount: number;
  todayProcessedCount: number;
}

export interface AdminAlerts {
  overdueCount: number;
  missingDocumentCount: number;
  duplicateSuspectCount: number;
}

export interface VerificationChecks {
  lawyerId: string;
  emailDuplicate: boolean;
  phoneDuplicate: boolean;
  nameDuplicate: boolean;
  requiredFields: boolean;
}

export const adminApi = {
  /** 대시보드 통계 */
  getStats: () =>
    api.get<ApiResponse<AdminStats>>(`${BASE}/dashboard/stats`),

  /** 긴급 알림 */
  getAlerts: () =>
    api.get<ApiResponse<AdminAlerts>>(`${BASE}/dashboard/alerts`),

  /** 심사 대기 변호사 목록 */
  getPendingLawyers: (page = 0, size = 20, keyword?: string, status?: string) =>
    api.get<ApiResponse<PageResponse<LawyerDetailResponse>>>(
      `${BASE}/lawyers/pending`,
      { params: { page, size, keyword, status } },
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
    api.get<ApiResponse<VerificationChecks>>(
      `${BASE}/lawyers/${id}/verification-checks`,
    ),

  /** 서류 조회 */
  getDocuments: (id: string) =>
    api.get<ApiResponse<Array<{ documentId: string; fileName: string; fileSize: number; fileType: string; fileUrl: string; uploadedAt: string }>>>(
      `${BASE}/lawyers/${id}/documents`,
    ),
};
