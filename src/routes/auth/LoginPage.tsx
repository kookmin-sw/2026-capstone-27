import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Shield, MessageCircle, Globe, Lock, Terminal, ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import { loginWithKakao } from '@/lib/kakao';
import { loginWithNaver } from '@/lib/naver';
import { loginWithGoogle } from '@/lib/google';
import { useAuthStore } from '@/stores/authStore';
import api from '@/lib/api';
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
      const { data } = await api.post('/auth/dev/login', {
        email: `dev-${devRole.toLowerCase()}@shield.dev`,
        name: `Dev ${devRole}`,
        role: devRole,
      });
      const { accessToken, refreshToken } = data.data;
      await login(accessToken, refreshToken);
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
    <div className="min-h-dvh flex flex-col">
      {/* ── Hero gradient area (≈55% of viewport) ── */}
      <div
        className={cn(
          'flex flex-col items-center justify-center gap-4',
          'bg-gradient-to-b from-blue-500 to-blue-600',
          'px-6 pt-16 pb-12',
        )}
        style={{ minHeight: '46vh' }}
      >
        {/* Shield logo */}
        <div className="flex items-center justify-center w-20 h-20 rounded-2xl bg-white shadow-lg">
          <Shield size={44} className="text-brand" strokeWidth={1.8} />
        </div>

        {/* Tagline */}
        <div className="text-center space-y-1.5">
          <h1 className="text-2xl font-bold text-white tracking-tight">
            더 스마트한 법률 파트너
          </h1>
          <p className="text-sm text-blue-100">
            AI 법률 정보 구조화 플랫폼
          </p>
        </div>

        {/* Security badge */}
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full',
            'bg-white/20 text-white text-xs font-medium',
          )}
        >
          <Lock size={12} className="flex-shrink-0" aria-hidden="true" />
          보안 인증 및 데이터 암호화
        </span>
      </div>

      {/* ── Social login area (≈45% of viewport) ── */}
      <div className="flex-1 flex flex-col justify-between px-5 pt-8 pb-8 bg-white">
        <div className="flex flex-col gap-3">
          {/* Kakao */}
          <Button
            variant="kakao"
            size="lg"
            fullWidth
            className="rounded-xl"
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
            className="rounded-xl"
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
            className="rounded-xl"
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
            구글로 시작하기
          </Button>
        </div>

        {/* Terms notice */}
        <p className="text-center text-xs text-[#94A3B8] leading-relaxed px-2 mt-6">
          로그인 시 SHIELD의{' '}
          <span className="text-[#64748B] font-medium">이용약관</span> 및{' '}
          <span className="text-[#64748B] font-medium">개인정보 처리방침</span>에
          동의하는 것으로 간주합니다.
        </p>

        {/* ── Dev Login Section ── */}
        <div className="mt-4 border-t border-dashed border-gray-200 pt-4">
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
