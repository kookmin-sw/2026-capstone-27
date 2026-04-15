import { Navigate } from 'react-router-dom';
import { Shield, MessageCircle, Globe, Lock } from 'lucide-react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import { loginWithKakao } from '@/lib/kakao';
import { loginWithNaver } from '@/lib/naver';
import { loginWithGoogle } from '@/lib/google';
import { useAuthStore } from '@/stores/authStore';

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

export function LoginPage() {
  const { isAuthenticated, role } = useAuthStore();

  if (isAuthenticated) {
    return <Navigate to={getRoleHome(role)} replace />;
  }

  return (
    <div className="flex flex-col">
      {/* ── Hero card ── */}
      <div className="flex flex-col items-center gap-3 pt-6 pb-8">
        {/* Shield icon */}
        <div className="flex items-center justify-center w-20 h-20 rounded-[24px] bg-white shadow-lg">
          <Shield size={44} className="text-brand" strokeWidth={1.8} />
        </div>

        {/* App name */}
        <h1 className="text-3xl font-bold tracking-tight text-white">
          SHIELD
        </h1>

        {/* Tagline */}
        <p className="text-base font-medium text-blue-100">
          더 스마트한 법률 파트너
        </p>

        {/* Secure badge */}
        <span
          className={cn(
            'inline-flex items-center gap-1.5 px-3 py-1 rounded-full',
            'bg-white/20 text-white text-xs font-medium',
          )}
        >
          <Lock size={11} className="flex-shrink-0" aria-hidden="true" />
          보안 인증 및 데이터 암호화
        </span>
      </div>

      {/* ── Login card ── */}
      <div className="bg-white rounded-card shadow-md px-6 py-8 flex flex-col gap-6">
        <div className="flex flex-col gap-1 text-center">
          <p className="text-base font-semibold text-[#1E293B]">소셜 계정으로 시작하기</p>
          <p className="text-sm text-[#64748B]">간편하게 로그인하고 법률 서비스를 이용하세요</p>
        </div>

        {/* Social login buttons */}
        <div className="flex flex-col gap-3">
          {/* Kakao */}
          <Button
            variant="kakao"
            size="lg"
            fullWidth
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
        <p className="text-center text-xs text-[#94A3B8] leading-relaxed px-2">
          계속 진행하면 SHIELD의{' '}
          <span className="text-[#64748B] font-medium">이용약관</span> 및{' '}
          <span className="text-[#64748B] font-medium">개인정보처리방침</span>에
          동의하는 것으로 간주됩니다.
        </p>
      </div>
    </div>
  );
}
