import api from './api';
import type { ApiResponse } from '@/types/api';
import type {
  LoginResponse,
  DevLoginRequest,
  GoogleLoginRequest,
  RefreshTokenResponse,
  SocialLoginRequest,
} from '@/types/auth';

const BASE = '/auth';

export const authApi = {
  /** 구글 로그인 */
  googleLogin: (data: GoogleLoginRequest) =>
    api.post<ApiResponse<LoginResponse>>(`${BASE}/google`, data, {
      withCredentials: true,
    }),

  /** 카카오 로그인 */
  kakaoLogin: (data: Pick<SocialLoginRequest, 'authorizationCode'>) =>
    api.post<ApiResponse<LoginResponse>>(`${BASE}/kakao`, data, {
      withCredentials: true,
    }),

  /** 네이버 로그인 */
  naverLogin: (data: Pick<SocialLoginRequest, 'authorizationCode' | 'state'>) =>
    api.post<ApiResponse<LoginResponse>>(`${BASE}/naver`, data, {
      withCredentials: true,
    }),

  /** 로그아웃 */
  logout: () =>
    api.post<ApiResponse<void>>(`${BASE}/logout`, null, {
      withCredentials: true,
    }),

  /** 개발용 로그인 */
  devLogin: (data: DevLoginRequest) =>
    api.post<ApiResponse<LoginResponse>>(`${BASE}/dev/login`, data),

  /** 토큰 갱신 */
  refreshToken: () =>
    api.post<ApiResponse<RefreshTokenResponse>>(`${BASE}/token/refresh`, null, {
      withCredentials: true,
    }),
};
