import { ArrowLeft, User, Mail, Phone, Briefcase, MapPin, CircleCheck } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button, Input, SpecializationPicker } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { cn } from '@/lib/cn';
import type { PendingRegistrationState } from '@/lib/authFlow';

// API 명세: POST /api/lawyers/me/register
//   - barAssociationNumber (필수)   ex) "KBA-2018-12345"
//   - domains / subDomains / tags   (온톨로지 L1/L2/L3)
//   - experienceYears, bio, region
//
// 이번 PR은 UI만 정리. 서버 연동은 다음 PR에서 추가한다.
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
  barAssociationNumber: z
    .string()
    .min(1, '대한변호사협회 등록번호를 입력해주세요'),
  // 현재 SpecializationPicker 는 L3(리프) 태그만 반환하므로
  // 일단 UI에서는 tags 로 모으고, 실제 서버 요청 시 도메인 매핑은 다음 PR에서 처리한다.
  tags: z.array(z.string()).min(1, '전문 분야를 1개 이상 선택해주세요'),
  experienceYears: z
    .number({ error: '경력 연수를 숫자로 입력해주세요' })
    .min(0, '경력은 0년 이상이어야 합니다'),
  bio: z.string().max(500, '자기소개는 500자 이하로 작성해주세요').optional().or(z.literal('')),
  region: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export function LawyerRegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);

  const pending = (location.state ?? null) as PendingRegistrationState | null;

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setValue,
    watch,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: pending?.name ?? '',
      email: pending?.email ?? '',
      phone: '',
      barAssociationNumber: '',
      tags: [],
      experienceYears: 0,
      bio: '',
      region: '',
    },
  });

  const selectedTags = watch('tags') ?? [];
  const bio = watch('bio') ?? '';

  const onSubmit = async (_data: FormValues) => {
    // TODO(next PR): POST /api/lawyers/me/register 연동
    // 현재 PR은 UI 플로우만 맞추므로 소셜 로그인으로 발급된 accessToken 만 적용하고
    // 변호사 홈으로 이동. 서버 실제 승인 상태는 다음 PR에서 verificationStatus 로 분기.
    if (pending?.accessToken) {
      await login(pending.accessToken);
    }

    navigate('/lawyer', { replace: true });
  };

  return (
    <div className="flex flex-col flex-1">
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
          변호사 회원가입
        </h1>
      </div>

      <div className="flex-1 px-6 pt-8 pb-6 overflow-y-auto">
        {/* Title */}
        <h2 className="text-2xl font-bold text-[#16181d] tracking-tight mb-2">전문 변호사 등록</h2>
        <p className="text-sm text-[#575e6b] leading-5 mb-6">
          SHIELD에서 변호사로 등록되기 위한
          <br />
          정보를 입력해주세요.
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6" noValidate>
          {/* ── Section: 기본 인적 사항 ── */}
          <div className="flex items-center gap-2 border-b border-[#e0e2e6] pb-2">
            <User size={16} className="text-[#575e6b]" />
            <span className="text-sm font-bold text-[#575e6b] uppercase tracking-wide">기본 인적 사항</span>
          </div>

          <Input
            label="성명"
            placeholder="홍길동"
            error={errors.name?.message}
            autoComplete="name"
            leftAddon={<User size={16} />}
            className="bg-white rounded-[10px]"
            {...register('name')}
          />

          <Input
            label="이메일 주소"
            type="email"
            placeholder="example@shield.com"
            error={errors.email?.message}
            autoComplete="email"
            leftAddon={<Mail size={16} />}
            className="bg-white rounded-[10px]"
            {...register('email')}
          />

          <Input
            label="연락처"
            type="tel"
            placeholder="010-0000-0000"
            error={errors.phone?.message}
            autoComplete="tel"
            leftAddon={<Phone size={16} />}
            className="bg-white rounded-[10px]"
            {...register('phone')}
          />

          <Input
            label="활동 지역"
            placeholder="서울특별시"
            error={errors.region?.message}
            leftAddon={<MapPin size={16} />}
            className="bg-white rounded-[10px]"
            {...register('region')}
          />

          {/* ── Section: 전문성 정보 ── */}
          <div className="flex items-center gap-2 border-b border-[#e0e2e6] pb-2 mt-2">
            <Briefcase size={16} className="text-[#575e6b]" />
            <span className="text-sm font-bold text-[#575e6b] uppercase tracking-wide">전문성 정보</span>
          </div>

          <Input
            label="대한변호사협회 등록번호"
            placeholder="예) KBA-2018-12345"
            error={errors.barAssociationNumber?.message}
            className="bg-white rounded-[10px]"
            {...register('barAssociationNumber')}
          />

          {/* Specializations (tags) */}
          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-[#16181d]">
              전문 분야 (하나 이상 선택)
            </span>
            <SpecializationPicker
              value={selectedTags}
              onChange={(v) => setValue('tags', v, { shouldValidate: true })}
              error={errors.tags?.message}
            />
          </div>

          {/* Experience years */}
          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-[#16181d]">경력 연수</span>
            <div className="flex items-center gap-2">
              <input
                id="experienceYears"
                type="number"
                min={0}
                placeholder="0"
                className={cn(
                  'w-full rounded-[10px] border bg-white px-3 py-2.5 text-sm text-[#16181d]',
                  'placeholder:text-[#575e6b] text-right',
                  'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
                  errors.experienceYears ? 'border-red-500' : 'border-[#e0e2e6]',
                )}
                {...register('experienceYears', { valueAsNumber: true })}
              />
              <span className="text-sm font-medium text-[#575e6b] shrink-0">년</span>
            </div>
            <p className="text-[11px] text-[#575e6b]">실제 법조 경력을 숫자로만 입력해주세요.</p>
            {errors.experienceYears && (
              <p className="text-xs text-red-500 leading-snug" role="alert">
                {errors.experienceYears.message}
              </p>
            )}
          </div>

          {/* Bio */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-[#16181d]">자기소개</span>
              <span className="text-[11px] text-[#575e6b]">{bio.length}/500</span>
            </div>
            <textarea
              placeholder="여기에 자기소개를 간략하게 작성해 주세요..."
              maxLength={500}
              rows={4}
              className={cn(
                'w-full rounded-[10px] border bg-white px-3 py-2.5 text-sm text-[#16181d]',
                'placeholder:text-[#575e6b] resize-none',
                'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
                errors.bio ? 'border-red-500' : 'border-[#e0e2e6]',
              )}
              {...register('bio')}
            />
            {errors.bio && (
              <p className="text-xs text-red-500 leading-snug" role="alert">
                {errors.bio.message}
              </p>
            )}
          </div>

          {/* Info banner */}
          <div className="flex gap-3 bg-[#f0f7ff] border border-brand/10 rounded-[14px] p-4">
            <CircleCheck size={20} className="text-brand shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-bold text-brand mb-1">자격 확인 후 승인됩니다</p>
              <p className="text-[11px] text-[#02264b] leading-5">
                등록된 정보를 바탕으로 SHIELD 검토팀에서 검토 후 승인해 드립니다. (1~3 영업일 내 처리)
              </p>
            </div>
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              isLoading={isSubmitting}
              className="rounded-[14px] h-14 text-lg"
            >
              회원가입 완료
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
