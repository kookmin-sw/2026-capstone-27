import type { UserRole } from '@/types';

/**
 * 역할별 기본 홈 경로.
 * role이 null/undefined이거나 USER이면 의뢰인 홈으로 폴백.
 */
export function getRoleHome(role?: UserRole | string | null): string {
  switch (role) {
    case 'LAWYER':
      return '/lawyer';
    case 'ADMIN':
      return '/admin';
    default:
      return '/home';
  }
}

interface SocialLoginRouteInput {
  isNewUser?: boolean;
  role?: UserRole | string | null;
}

/**
 * 소셜 로그인 성공 후 이동할 경로 결정.
 *
 * 사양 기반:
 *   - 신규 사용자(isNewUser=true) → /role-select (의뢰인/변호사 선택)
 *   - 그 외 → 역할별 홈
 *
 * 현재 API 명세에서 isNewUser가 선택 필드라 서버 구현 전에는 누락될 수 있음.
 * 그 경우를 대비해, role이 있으면 역할별 홈으로 보내는 것을 최종 판단 소스로 사용.
 */
export function routeAfterSocialLogin({ isNewUser, role }: SocialLoginRouteInput): string {
  if (isNewUser) {
    return '/role-select';
  }
  return getRoleHome(role ?? null);
}

/**
 * 역할 선택 화면에서 register 페이지로 넘길 때 state로 전달하는 구조.
 */
export interface PendingRegistrationState {
  accessToken: string;
  name?: string;
  email?: string;
  provider?: 'google' | 'kakao' | 'naver';
}
