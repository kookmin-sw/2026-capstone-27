import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spinner } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/lib/authApi';
import { getRoleHome, routeAfterSocialLogin } from '@/lib/authFlow';

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
        // 명세: POST /api/auth/google { authorizationCode }
        // 최초 로그인 시 서버가 기본 USER로 가입시킴. role 지정이 필요한 경우는
        // 역할 선택 화면(/role-select)을 거쳐 해당 역할로 재요청하는 플로우.
        const { data } = await authApi.googleLogin({ authorizationCode: code });

        const payload = data.data;
        const { accessToken, isNewUser, role, name, email } = payload;

        await login(accessToken);

        // 신규 사용자면 역할 선택 화면으로, 아니면 역할별 홈으로
        const next = routeAfterSocialLogin({ isNewUser, role });
        if (next === '/role-select') {
          navigate(next, {
            replace: true,
            state: { accessToken, name, email, provider: 'google' },
          });
          return;
        }
        navigate(getRoleHome(role), { replace: true });
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
    <div className="flex-1 flex flex-col items-center justify-center gap-4 px-4">
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
