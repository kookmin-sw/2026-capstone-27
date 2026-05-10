import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { Loader2 } from 'lucide-react';
import type { PendingRegistrationState } from '@/lib/authFlow';

/**
 * 온보딩 전용 라우트 가드.
 *
 * 대상 경로: /role-select, /register/client, /register/lawyer
 *
 * 이 경로들은 "소셜 로그인 콜백 직후의 중간 단계"로만 의미가 있음.
 * 아래 두 경우에만 통과를 허용:
 *   1) 이미 인증된 사용자 (accessToken 이 스토어에 있음) — 역할 미확정 상태에서 화면을 완성하러 돌아오는 경우
 *   2) location.state.accessToken 이 있는 경우 — 콜백 페이지가 RoleSelect/Register 로 넘길 때의 정상 경로
 *
 * 둘 다 아니면 /login 으로 보낸다 (직접 URL 로 접근하는 비정상 플로우 차단).
 */
export function OnboardingRoute() {
  const { isAuthenticated, isLoading } = useAuthStore();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-dvh flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-brand" />
      </div>
    );
  }

  const state = (location.state ?? null) as PendingRegistrationState | null;
  const hasPendingToken = Boolean(state?.accessToken);

  if (!isAuthenticated && !hasPendingToken) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
