import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Shield, MessageCircle, Globe, Lock, Terminal, ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import { loginWithKakao } from '@/lib/kakao';
import { loginWithNaver } from '@/lib/naver';
import { loginWithGoogle } from '@/lib/google';
import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/lib/authApi';
import type { UserRole } from '@/types';

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
    <div className="min-h-dvh flex flex-col bg-white relative overflow-hidden">
      {/* Decorative blur */}
      <div className="absolute -top-20 right-0 w-64 h-64 rounded-full bg-brand/5 blur-[64px]" />

      {/* ── Brand area ── */}
      <div className="flex flex-col items-center pt-[162px] pb-8 px-6 gap-4">
        {/* Shield logo — blue square with white icon */}
        <div className="w-[84px] h-[84px] rounded-2xl bg-brand flex items-center justify-center">
          <Shield size={48} className="text-white" strokeWidth={1.5} />
        </div>

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
          <Lock size={12} className="flex-shrink-0" aria-hidden="true" />
          보안 인증 및 데이터 암호화
        </span>
      </div>

      {/* ── Social login area ── */}
      <div className="flex-1 flex flex-col justify-end px-6 pb-8 gap-6">
        <div className="flex flex-col gap-[12px]">
          {/* Kakao */}
          <Button
            variant="kakao"
            size="lg"
            fullWidth
            className="rounded-[14px] h-14 text-base font-semibold shadow-[0px_2px_4px_0px_rgba(35,37,41,0.06)]"
            leftIcon={<MessageCircle size={20} aria-hidden="true" />}
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
            leftIcon={
              <span
                className="flex items-center justify-center w-5 h-5 font-bold text-sm leading-none text-[#4285F4]"
                aria-hidden="true"
              >
                <Globe size={20} />
              </span>
            }
            onClick={loginWithGoogle}
          >
            Google 계정으로 시작하기
          </Button>
        </div>

        {/* Terms notice */}
        <p className="text-center text-[10px] text-[#575e6b] leading-[18px] px-1">
          로그인 시 SHIELD의{' '}
          <span className="underline font-medium">이용약관</span> 및{' '}
          <span className="underline font-medium">개인정보 처리방침</span>에
          동의하는 것으로 간주합니다.
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
