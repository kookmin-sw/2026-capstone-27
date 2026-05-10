import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ShieldCheck, Terminal, ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import { loginWithKakao } from '@/lib/kakao';
import { loginWithNaver } from '@/lib/naver';
import { loginWithGoogle } from '@/lib/google';
import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/lib/authApi';
import type { UserRole } from '@/types';

// 소셜 로그인 버튼 아이콘 — lucide 아이콘은 인증 기관 고유 로고와 달라
// 식별성을 높이기 위해 인라인 SVG로 직접 렌더링한다.
function KakaoIcon({ size = 20 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        d="M12 3.5C6.753 3.5 2.5 6.84 2.5 10.96c0 2.6 1.706 4.888 4.286 6.225l-1.01 3.69a.44.44 0 0 0 .666.49l4.38-2.907c.39.05.78.082 1.178.082 5.247 0 9.5-3.34 9.5-7.46S17.247 3.5 12 3.5Z"
        fill="#000"
      />
    </svg>
  );
}

function GoogleIcon({ size = 20 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        fill="#FFC107"
        d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917Z"
      />
      <path
        fill="#FF3D00"
        d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691Z"
      />
      <path
        fill="#4CAF50"
        d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238A11.91 11.91 0 0 1 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44Z"
      />
      <path
        fill="#1976D2"
        d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917Z"
      />
    </svg>
  );
}

function getRoleHome(role: string | null): string {
  switch (role) {
    case 'LAWYER':
      return '/lawyer';
    case 'ADMIN':
      return '/admin';
    default:
      return '/home';
  }
}

const DEV_ROLES: { role: UserRole; label: string; color: string }[] = [
  { role: 'USER', label: '의뢰인', color: 'bg-emerald-500 hover:bg-emerald-600' },
  { role: 'LAWYER', label: '변호사', color: 'bg-violet-500 hover:bg-violet-600' },
  { role: 'ADMIN', label: '관리자', color: 'bg-rose-500 hover:bg-rose-600' },
];

export function LoginPage() {
  const { isAuthenticated, role, login } = useAuthStore();
  const [devOpen, setDevOpen] = useState(false);
  const [devLoading, setDevLoading] = useState<UserRole | null>(null);

  async function handleDevLogin(devRole: UserRole) {
    setDevLoading(devRole);
    try {
      const { data } = await authApi.devLogin({
        email: `dev-${devRole.toLowerCase()}@shield.dev`,
        name: `Dev ${devRole}`,
        role: devRole,
      });
      const { accessToken } = data.data;
      await login(accessToken);
    } catch (err) {
      console.error('Dev login failed:', err);
    } finally {
      setDevLoading(null);
    }
  }

  if (isAuthenticated) {
    return <Navigate to={getRoleHome(role)} replace />;
  }

  return (
    <div className="flex flex-col flex-1 relative overflow-hidden">
      {/* Decorative blur */}
      <div className="absolute -top-20 right-0 w-64 h-64 rounded-full bg-brand/5 blur-[64px]" />

      {/* ── Brand area ── */}
      <div className="flex flex-col items-center pt-[96px] sm:pt-[120px] pb-8 px-6 gap-4">
        {/* SHIELD logo */}
        <img
          src="/logo.png"
          alt="SHIELD"
          className="w-[88px] h-[88px] object-contain"
        />

        {/* Tagline */}
        <div className="text-center space-y-1">
          <h1 className="text-xl font-bold text-[#16181d] tracking-tight">
            더 스마트한 법률 파트너
          </h1>
          <p className="text-sm text-[#575e6b]">
            AI 법률 정보 구조화 플랫폼
          </p>
        </div>

        {/* Security badge */}
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-4 py-2 rounded-full',
            'bg-[#f0f7ff] border border-brand/10 text-brand text-[11px] font-normal',
          )}
        >
          <ShieldCheck size={12} className="shrink-0" aria-hidden="true" />
          보안 인증 및 데이터 암호화
        </span>
      </div>

      {/* ── Social login area ── */}
      <div className="flex-1 flex flex-col justify-end sm:justify-center px-6 pb-8 sm:pb-12 gap-6">
        <div className="flex flex-col gap-[12px]">
          {/* Kakao */}
          <Button
            variant="kakao"
            size="lg"
            fullWidth
            className="rounded-[14px] h-14 text-base font-semibold shadow-[0px_2px_4px_0px_rgba(35,37,41,0.06)]"
            leftIcon={<KakaoIcon size={20} />}
            onClick={loginWithKakao}
          >
            카카오로 시작하기
          </Button>

          {/* Naver */}
          <Button
            variant="naver"
            size="lg"
            fullWidth
            className="rounded-[14px] h-14 text-base font-semibold shadow-[0px_2px_4px_0px_rgba(35,37,41,0.06)]"
            leftIcon={
              <span
                className="flex items-center justify-center w-5 h-5 rounded font-extrabold text-sm leading-none text-white"
                aria-hidden="true"
              >
                N
              </span>
            }
            onClick={loginWithNaver}
          >
            네이버로 시작하기
          </Button>

          {/* Google */}
          <Button
            variant="google"
            size="lg"
            fullWidth
            className="rounded-[14px] h-14 text-base font-semibold shadow-[0px_2px_4px_0px_rgba(35,37,41,0.06)]"
            leftIcon={<GoogleIcon size={20} />}
            onClick={loginWithGoogle}
          >
            Google 계정으로 시작하기
          </Button>
        </div>

        {/* Terms notice */}
        <p className="text-center text-xs text-[#575e6b] leading-4.5 px-1">
          로그인 시 SHIELD의{' '}
          <a
            href="/terms"
            className="text-brand underline font-medium hover:opacity-80"
          >
            이용약관
          </a>{' '}
          및{' '}
          <a
            href="/privacy"
            className="text-brand underline font-medium hover:opacity-80"
          >
            개인정보 처리방침
          </a>
          에 동의하는 것으로 간주합니다.
        </p>

        {/* ── Dev Login Section ── */}
        <div className="border-t border-dashed border-gray-200 pt-4">
          <button
            type="button"
            onClick={() => setDevOpen(!devOpen)}
            className={cn(
              'flex items-center justify-center gap-1.5 w-full',
              'text-xs text-gray-400 hover:text-gray-600 transition-colors',
            )}
          >
            <Terminal size={12} />
            <span>Dev Login</span>
            {devOpen ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>

          {devOpen && (
            <div className="mt-3 flex gap-2">
              {DEV_ROLES.map(({ role: devRole, label, color }) => (
                <button
                  key={devRole}
                  type="button"
                  disabled={devLoading !== null}
                  onClick={() => handleDevLogin(devRole)}
                  className={cn(
                    'flex-1 py-2 rounded-lg text-xs font-semibold text-white transition-all',
                    color,
                    devLoading === devRole && 'opacity-60 animate-pulse',
                    devLoading !== null && devLoading !== devRole && 'opacity-40',
                  )}
                >
                  {devLoading === devRole ? '...' : label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
