import api from './api';
import type { ApiResponse, PageResponse } from '@/types/api';
import type {
  LawyerResponse,
  LawyerDetailResponse,
  LawyerMeResponse,
  ProfileUpdateRequest,
  VerificationStatusResponse,
  VerificationRequestData,
  DocumentResponse,
} from '@/types/lawyer';
import type { RegisterLawyerRequest, RegisterLawyerResponse } from '@/types/auth';

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

  /** 프로필 수정 (변호사) — BE ProfileUpdateRequest 와 정합 맞춤 */
  updateMe: (data: ProfileUpdateRequest) =>
    api.patch<ApiResponse<LawyerMeResponse>>(`${BASE}/me`, data),

  /** 검증 상태 확인 */
  getVerificationStatus: () =>
    api.get<ApiResponse<VerificationStatusResponse>>(`${BASE}/me/verification-status`),

  /** 검증 신청 */
  requestVerification: (data: VerificationRequestData) =>
    api.post<ApiResponse<void>>(`${BASE}/me/verification-request`, data),

  /**
   * 변호사 등록
   * 명세: POST /api/lawyers/me/register
   *   - 서버가 User.role 을 LAWYER 로 승격 + LawyerProfile 생성 + 새 JWT 재발급
   *   - 응답 data.accessToken 으로 토큰 교체 필수
   *   - 새 refreshToken 은 HttpOnly 쿠키로 내려오므로 withCredentials 필수
   */
  register: (data: RegisterLawyerRequest) =>
    api.post<ApiResponse<RegisterLawyerResponse>>(`${BASE}/me/register`, data, {
      withCredentials: true,
    }),

  /** 본인 서류 목록 조회 */
  getMyDocuments: () =>
    api.get<ApiResponse<DocumentResponse[]>>(`${BASE}/me/documents`),

  /** 서류 업로드 */
  uploadDocument: (formData: FormData) =>
    api.post<ApiResponse<DocumentResponse>>(`${BASE}/me/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
