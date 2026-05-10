import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { DocumentResponse } from '@/types/lawyer';
import type {
  AdminStats,
  AdminAlerts,
  PendingLawyerResponse,
  AdminLawyerDetailResponse,
  VerificationChecks,
  VerificationLog,
  VerificationActionRequest,
  VerificationActionResponse,
  VerificationLogsFilter,
} from '@/types/admin';

// 구 코드와 호환 위해 re-export (이미 adminApi 로부터 import 해온 사용처 있음).
export type {
  AdminStats,
  AdminAlerts,
  PendingLawyerResponse,
  AdminLawyerDetailResponse,
  VerificationChecks,
  VerificationLog,
  VerificationActionRequest,
  VerificationActionResponse,
  VerificationLogsFilter,
};

const BASE = '/admin';

export const adminApi = {
  /** 대시보드 통계 */
  getStats: () =>
    api.get<ApiResponse<AdminStats>>(`${BASE}/dashboard/stats`),

  /** 긴급 알림 */
  getAlerts: () =>
    api.get<ApiResponse<AdminAlerts>>(`${BASE}/dashboard/alerts`),

  /** 심사 대기 변호사 목록 — 응답은 PendingLawyerResponse 로 email/phone/documentCount 포함 */
  getPendingLawyers: (page = 0, size = 20, keyword?: string, status?: string) =>
    api.get<ApiResponse<PageResponse<PendingLawyerResponse>>>(
      `${BASE}/lawyers/pending`,
      { params: { page, size, keyword, status } },
    ),

  /** 변호사 상세 (관리자) — 공용 LawyerResponse 가 아니라 email/phone/barAssociationNumber 등 포함 */
  getLawyerDetail: (id: string) =>
    api.get<ApiResponse<AdminLawyerDetailResponse>>(`${BASE}/lawyers/${id}`),

  /** 심사 처리 — BE 는 VerificationResponse 를 반환한다 (void 아님) */
  processVerification: (id: string, data: VerificationActionRequest) =>
    api.patch<ApiResponse<VerificationActionResponse>>(
      `${BASE}/lawyers/${id}/verification`,
      data,
    ),

  /** 자동 검증 결과 */
  getVerificationChecks: (id: string) =>
    api.get<ApiResponse<VerificationChecks>>(
      `${BASE}/lawyers/${id}/verification-checks`,
    ),

  /** 서류 조회 — lawyerApi.getMyDocuments 와 동일한 DocumentResponse DTO */
  getDocuments: (id: string) =>
    api.get<ApiResponse<DocumentResponse[]>>(`${BASE}/lawyers/${id}/documents`),

  /**
   * 처리 이력 조회 — BE 는 period(today/week) 와 status 를 따로 받는다.
   * startDate/endDate 같은 임의 날짜 범위는 지원하지 않는다.
   */
  getVerificationLogs: (page = 0, size = 20, filters?: VerificationLogsFilter) =>
    api.get<ApiResponse<PageResponse<VerificationLog>>>(
      `${BASE}/verification-logs`,
      { params: { page, size, period: filters?.period, status: filters?.status } },
    ),
};
