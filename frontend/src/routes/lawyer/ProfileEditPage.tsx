import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { cn } from '@/lib/cn';
import { lawyerApi } from '@/lib/lawyerApi';
import { Button, Card, Spinner, SpecializationPicker } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { LawyerResponse } from '@/types/lawyer';

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

  // 변경하지 않는 필드(subDomains, certifications, tags, bio, region) 는 서버 값을 그대로 전송해야 한다.
  const profileRef = useRef<LawyerResponse | null>(null);

  // Fetch current profile on mount
  useEffect(() => {
    lawyerApi.getMe().then(({ data }) => {
      const profile = data.data;
      profileRef.current = profile;
      reset({
        specializations: profile.domains ?? [],
        experienceYears: profile.experienceYears ?? 0,
      });
    });
  }, [reset]);

  async function onSubmit(values: FormValues) {
    const profile = profileRef.current;
    await lawyerApi.updateMe({
      domains: values.specializations,
      subDomains: profile?.subDomains ?? [],
      experienceYears: values.experienceYears,
      certifications: profile?.certifications ?? [],
      tags: profile?.tags ?? [],
      bio: profile?.bio ?? '',
      region: profile?.region ?? '',
    });
    alert('프로필이 저장되었습니다.');
    navigate('/lawyer/profile');
  }

  if (isLoading) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="프로필 수정" showBack onBack={() => navigate(-1)} />
        <div className="flex items-center justify-center flex-1">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1">
      <Header title="프로필 수정" showBack onBack={() => navigate(-1)} />

      <main className="flex-1 px-4 py-4 pb-10">
        <Card padding="md">
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
            {/* Specializations */}
            <div className="flex flex-col gap-1.5">
              <span className="text-sm font-medium text-[#1E293B] leading-none">
                전문분야
              </span>
              <SpecializationPicker
                value={selectedSpecs}
                onChange={(v) => setValue('specializations', v, { shouldValidate: true })}
                error={errors.specializations?.message}
              />
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
