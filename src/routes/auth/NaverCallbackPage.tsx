import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spinner } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { validateNaverState } from '@/lib/naver';
import api from '@/lib/api';

function getRoleHome(role: string): string {
  switch (role) {
    case 'LAWYER':
      return '/lawyer';
    case 'ADMIN':
      return '/admin';
    default:
      return '/home';
  }
}

export function NaverCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuthStore();
  const calledRef = useRef(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    // Guard against double-calls (StrictMode / re-renders)
    if (calledRef.current) return;
    calledRef.current = true;

    const code = searchParams.get('code');
    const state = searchParams.get('state');

    // Validate CSRF state before anything else
    if (!validateNaverState(state)) {
      navigate('/login', { replace: true, state: { error: 'invalid_state' } });
      return;
    }

    if (!code) {
      navigate('/login', { replace: true, state: { error: 'authorization_code_missing' } });
      return;
    }

    (async () => {
      try {
        const { data } = await api.post<{
          data: {
            accessToken: string;
            userId: string;
            name: string;
            role: string;
          };
        }>('/auth/naver', { authorizationCode: code, state }, { withCredentials: true });

        const { accessToken, role } = data.data;

        await login(accessToken);
        navigate(getRoleHome(role), { replace: true });
      } catch (err) {
        console.error('[NaverCallback] Error:', err);
        setErrorMsg('네이버 로그인에 실패했습니다. 잠시 후 다시 시도해주세요.');
        setTimeout(() => {
          navigate('/login', { replace: true, state: { error: 'naver_auth_failed' } });
        }, 2000);
      }
    })();
  }, [searchParams, navigate, login]);

  return (
    <div className="min-h-dvh flex flex-col items-center justify-center gap-4 px-4">
      {errorMsg ? (
        <div className="flex flex-col items-center gap-3 text-center">
          <p className="text-sm text-[#EF4444] font-medium">{errorMsg}</p>
          <p className="text-xs text-[#64748B]">로그인 페이지로 이동합니다...</p>
        </div>
      ) : (
        <Spinner size="lg" text="로그인 처리 중..." />
      )}
    </div>
  );
}
