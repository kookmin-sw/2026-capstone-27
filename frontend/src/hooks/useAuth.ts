import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/lib/authApi';
import { useAuthStore } from '@/stores/authStore';
import type { DevLoginRequest, GoogleLoginRequest } from '@/types/auth';

/** 구글 로���인 */
export function useGoogleLogin() {
  const { login } = useAuthStore();
  return useMutation({
    mutationFn: (data: GoogleLoginRequest) => authApi.googleLogin(data),
    onSuccess: async ({ data }) => {
      await login(data.data.accessToken);
    },
  });
}

/** 로그아웃 — authStore.logout()이 서버 호출 + 토큰 정리를 모두 처리 */
export function useLogout() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { logout } = useAuthStore();

  return useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.clear();
      navigate('/login', { replace: true });
    },
  });
}

/** 개발용 로그인 */
export function useDevLogin() {
  const { login } = useAuthStore();
  return useMutation({
    mutationFn: (data: DevLoginRequest) => authApi.devLogin(data),
    onSuccess: async ({ data }) => {
      await login(data.data.accessToken);
    },
  });
}
