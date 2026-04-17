import { ArrowLeft, User, Mail, Phone, Info } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useState } from 'react';
import { Button, Input } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { cn } from '@/lib/cn';

const schema = z.object({
  name: z.string().min(1, '이름을 입력해주세요'),
  email: z
    .string()
    .min(1, '이메일을 입력해주세요')
    .email('올바른 이메일 형식을 입력해주세요'),
  phone: z
    .string()
    .min(1, '전화번호를 입력해주세요')
    .regex(/^010-\d{4}-\d{4}$/, '010-XXXX-XXXX 형식으로 입력해주세요'),
});

type FormValues = z.infer<typeof schema>;

export function ClientRegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);
  const [agreed, setAgreed] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', email: '', phone: '' },
  });

  const onSubmit = async (_data: FormValues) => {
    const state = location.state as
      | { accessToken?: string; refreshToken?: string }
      | undefined;

    if (state?.accessToken && state?.refreshToken) {
      await login(state.accessToken);
    }

    navigate('/home', { replace: true });
  };

  return (
    <div className="flex flex-col min-h-dvh bg-white">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-white border-b border-[#e0e2e6] flex items-center h-17 px-2">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className={cn(
            'flex items-center justify-center w-10 h-10 rounded-full',
            'text-[#16181d] hover:bg-gray-100',
            'transition-colors duration-150',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
          )}
          aria-label="뒤로 가기"
        >
          <ArrowLeft size={24} />
        </button>
        <h1 className="flex-1 text-center text-lg font-semibold text-[#16181d] pr-10">
          의뢰인 회원가입
        </h1>
      </div>

      <div className="flex-1 px-6 pt-8 pb-6">
        {/* Welcome */}
        <h2 className="text-2xl font-bold text-[#16181d] tracking-tight mb-2">반갑습니다!</h2>
        <p className="text-sm text-[#575e6b] leading-6 mb-8">
          SHIELD의 AI 법률 서비스를 이용하기 위해
          <br />
          기본 정보를 입력해 주세요.
        </p>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
          <Input
            label="성함"
            placeholder="홍길동"
            error={errors.name?.message}
            autoComplete="name"
            leftAddon={<User size={16} />}
            className="bg-[#f9fafb] rounded-xl"
            {...register('name')}
          />

          <Input
            label="이메일 주소"
            type="email"
            placeholder="example@shield.ai"
            error={errors.email?.message}
            autoComplete="email"
            leftAddon={<Mail size={16} />}
            className="bg-[#f9fafb] rounded-xl"
            {...register('email')}
          />

          <Input
            label="휴대폰 번호"
            type="tel"
            placeholder="010-1234-5678"
            error={errors.phone?.message}
            autoComplete="tel"
            leftAddon={<Phone size={16} />}
            className="bg-[#f9fafb] rounded-xl"
            {...register('phone')}
          />

          {/* Info banner */}
          <div className="flex gap-3 bg-[#f0f7ff] border border-brand/10 rounded-[14px] p-4">
            <Info size={20} className="text-brand shrink-0 mt-0.5" />
            <p className="text-sm text-[#02264b] leading-5.75">
              입력하신 정보는 변호사 상담 및 본인 확인 용도로만 안전하게 사용됩니다.
            </p>
          </div>

          {/* Agreement checkbox */}
          <label className="flex items-start gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="mt-0.5 w-5 h-5 rounded-sm border-[#565d6d] text-brand focus:ring-brand/40"
            />
            <span className="text-sm font-medium text-[#16181d]/80">
              서비스 이용약관 및 개인정보 처리방침에 동의합니다.
            </span>
          </label>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              disabled={!agreed}
              isLoading={isSubmitting}
              className="rounded-xl h-14 text-lg"
            >
              회원가입 완료
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
