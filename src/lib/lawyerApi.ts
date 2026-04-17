import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type {
  LawyerResponse,
  LawyerDetailResponse,
  LawyerMeResponse,
  VerificationStatusResponse,
  VerificationRequestData,
  DocumentResponse,
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

  /** 검증 상태 확인 */
  getVerificationStatus: () =>
    api.get<ApiResponse<VerificationStatusResponse>>(`${BASE}/me/verification-status`),

  /** 검증 신청 */
  requestVerification: (data: VerificationRequestData) =>
    api.post<ApiResponse<void>>(`${BASE}/me/verification-request`, data),

  /** 본인 서류 목록 조회 */
  getMyDocuments: () =>
    api.get<ApiResponse<DocumentResponse[]>>(`${BASE}/me/documents`),

  /** 서류 업로드 */
  uploadDocument: (formData: FormData) =>
    api.post<ApiResponse<DocumentResponse>>(`${BASE}/me/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
