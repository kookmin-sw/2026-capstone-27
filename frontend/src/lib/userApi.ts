import api from './api';
import type { ApiResponse } from '@/types/api';
import type { UserInfo } from '@/types/auth';

const BASE = '/users';

export const userApi = {
  /** 내 정보 조회 */
  getMe: () =>
    api.get<ApiResponse<UserInfo>>(`${BASE}/me`),

  /** 내 정보 수정 */
  updateMe: (data: Partial<Pick<UserInfo, 'name' | 'email'>>) =>
    api.patch<ApiResponse<UserInfo>>(`${BASE}/me`, data),
};
