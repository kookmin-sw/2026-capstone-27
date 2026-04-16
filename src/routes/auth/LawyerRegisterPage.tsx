import { ArrowLeft } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button, Input } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { cn } from '@/lib/cn';

const SPECIALIZATIONS = ['민사', '형사', '노동', '학교폭력', '가사', '행정', '헌법'] as const;

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
  specializations: z
    .array(z.string())
    .min(1, '전문분야를 1개 이상 선택해주세요'),
  experienceYears: z
    .number()
    .min(0, '경력은 0년 이상이어야 합니다'),
  licenseNumber: z.string().min(1, '변호사 자격번호를 입력해주세요'),
});

type FormValues = z.infer<typeof schema>;

export function LawyerRegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setValue,
    watch,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      email: '',
      phone: '',
      specializations: [],
      experienceYears: 0,
      licenseNumber: '',
    },
  });

  const selectedSpecs = watch('specializations') ?? [];

  const toggleSpecialization = (spec: string) => {
    const current = selectedSpecs;
    if (current.includes(spec)) {
      setValue('specializations', current.filter((s) => s !== spec), {
        shouldValidate: true,
      });
    } else {
      setValue('specializations', [...current, spec], { shouldValidate: true });
    }
  };

  const onSubmit = async (_data: FormValues) => {
    const state = location.state as
      | { accessToken?: string; refreshToken?: string }
      | undefined;

    if (state?.accessToken && state?.refreshToken) {
      await login(state.accessToken);
    }

    navigate('/lawyer', { replace: true });
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
        <h1 className="text-xl font-bold text-[#1E293B]">변호사 회원가입</h1>
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

        {/* Specializations multi-select */}
        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-[#1E293B] leading-none">
            전문분야
          </span>
          <div className="flex flex-wrap gap-2 pt-0.5">
            {SPECIALIZATIONS.map((spec) => {
              const isActive = selectedSpecs.includes(spec);
              return (
                <button
                  key={spec}
                  type="button"
                  onClick={() => toggleSpecialization(spec)}
                  className={cn(
                    'px-3 py-1.5 rounded-full text-sm font-medium',
                    'transition-colors duration-150',
                    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
                    isActive
                      ? 'bg-brand text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200',
                  )}
                >
                  {spec}
                </button>
              );
            })}
          </div>
          {errors.specializations && (
            <p className="text-xs text-red-500 leading-snug" role="alert">
              {errors.specializations.message}
            </p>
          )}
        </div>

        {/* Experience years */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="experienceYears"
            className="text-sm font-medium text-[#1E293B] leading-none"
          >
            경력 (년)
          </label>
          <input
            id="experienceYears"
            type="number"
            min={0}
            placeholder="0"
            className={cn(
              'w-full rounded-xl border bg-white px-3 py-2.5 text-sm text-[#1E293B]',
              'placeholder:text-[#64748B]',
              'transition-colors duration-150',
              'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
              errors.experienceYears
                ? 'border-red-500 focus:ring-red-400/30 focus:border-red-500'
                : 'border-gray-300',
            )}
            {...register('experienceYears', { valueAsNumber: true })}
          />
          {errors.experienceYears && (
            <p className="text-xs text-red-500 leading-snug" role="alert">
              {errors.experienceYears.message}
            </p>
          )}
        </div>

        <Input
          label="변호사 자격번호"
          placeholder="자격번호를 입력해주세요"
          error={errors.licenseNumber?.message}
          {...register('licenseNumber')}
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
