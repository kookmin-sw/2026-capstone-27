import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spinner } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/lib/authApi';

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

export function GoogleCallbackPage() {
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

    if (!code) {
      navigate('/login', { replace: true, state: { error: 'authorization_code_missing' } });
      return;
    }

    (async () => {
      try {
        // 명세: POST /api/auth/google { authorizationCode, role }
        // role은 첫 로그인 시에만 필요 — 기존 사용자는 서버가 무시
        const { data } = await authApi.googleLogin({ authorizationCode: code });

        const { accessToken, role } = data.data;

        await login(accessToken);
        navigate(getRoleHome(role ?? ''), { replace: true });
      } catch (err) {
        console.error('[GoogleCallback] Error:', err);
        setErrorMsg('구글 로그인에 실패했습니다. 잠시 후 다시 시도해주세요.');
        setTimeout(() => {
          navigate('/login', { replace: true, state: { error: 'google_auth_failed' } });
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
