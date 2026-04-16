import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { cn } from '@/lib/cn';
import { lawyerApi } from '@/lib/lawyerApi';
import { Button, Card, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';

// ─── constants ───────────────────────────────────────────────────────────────

const SPECIALIZATIONS = ['민사', '형사', '노동', '학교폭력', '가사', '행정', '헌법'] as const;

// ─── schema ──────────────────────────────────────────────────────────────────

const schema = z.object({
  specializations: z
    .array(z.string())
    .min(1, '전문분야를 1개 이상 선택해주세요'),
  experienceYears: z
    .number()
    .min(0, '경력은 0년 이상이어야 합니다'),
});

type FormValues = z.infer<typeof schema>;

// ─── page ────────────────────────────────────────────────────────────────────

export function ProfileEditPage() {
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting, isLoading },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      specializations: [],
      experienceYears: 0,
    },
  });

  const selectedSpecs = watch('specializations') ?? [];

  // Fetch current profile on mount
  useEffect(() => {
    lawyerApi.getMe().then(({ data }) => {
      const profile = data.data;
      reset({
        specializations: profile.specializations ?? [],
        experienceYears: profile.experienceYears ?? 0,
      });
    });
  }, [reset]);

  function toggleSpecialization(spec: string) {
    if (selectedSpecs.includes(spec)) {
      setValue('specializations', selectedSpecs.filter((s) => s !== spec), {
        shouldValidate: true,
      });
    } else {
      setValue('specializations', [...selectedSpecs, spec], {
        shouldValidate: true,
      });
    }
  }

  async function onSubmit(values: FormValues) {
    await lawyerApi.updateMe({
      specializations: values.specializations,
      experienceYears: values.experienceYears,
    });
    alert('프로필이 저장되었습니다.');
    navigate('/lawyer/profile');
  }

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-dvh bg-surface">
        <Header title="프로필 수정" showBack onBack={() => navigate(-1)} />
        <div className="flex items-center justify-center flex-1">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="프로필 수정" showBack onBack={() => navigate(-1)} />

      <main className="flex-1 px-4 py-4 pb-10">
        <Card padding="md">
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
            {/* Specializations */}
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

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                fullWidth
                isLoading={isSubmitting}
              >
                저장
              </Button>
            </div>
          </form>
        </Card>
      </main>
    </div>
  );
}
