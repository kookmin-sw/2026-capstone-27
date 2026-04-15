import { ArrowLeft } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
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

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', email: '', phone: '' },
  });

  const onSubmit = async (_data: FormValues) => {
    // Tokens are passed from OAuth callback via location.state
    const state = location.state as
      | { accessToken?: string; refreshToken?: string }
      | undefined;

    if (state?.accessToken && state?.refreshToken) {
      await login(state.accessToken, state.refreshToken);
    }

    navigate('/home', { replace: true });
  };

  return (
    <div className="px-4 py-6 max-w-md mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 mb-8">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className={cn(
            'flex items-center justify-center w-9 h-9 rounded-full',
            'text-[#64748B] hover:text-[#1E293B] hover:bg-gray-100',
            'transition-colors duration-150',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
          )}
          aria-label="뒤로 가기"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-xl font-bold text-[#1E293B]">의뢰인 회원가입</h1>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
        <Input
          label="이름"
          placeholder="홍길동"
          error={errors.name?.message}
          autoComplete="name"
          {...register('name')}
        />

        <Input
          label="이메일"
          type="email"
          placeholder="example@email.com"
          error={errors.email?.message}
          autoComplete="email"
          {...register('email')}
        />

        <Input
          label="전화번호"
          type="tel"
          placeholder="010-0000-0000"
          error={errors.phone?.message}
          autoComplete="tel"
          {...register('phone')}
        />

        <div className="pt-2">
          <Button
            type="submit"
            variant="primary"
            size="lg"
            fullWidth
            isLoading={isSubmitting}
          >
            가입 완료
          </Button>
        </div>
      </form>
    </div>
  );
}
