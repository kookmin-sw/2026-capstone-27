import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { LogOut, FileText, Shield, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useAuthStore } from '@/stores/authStore';
import { useMyLawyerProfile } from '@/hooks/useLawyer';
import { lawyerApi } from '@/lib/lawyerApi';
import { Button, Spinner, SpecializationPicker } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { VerificationStatus } from '@/types/enums';

// ─── schema ──────────────────────────────────────────────────────────────────

const schema = z.object({
  specializations: z.array(z.string()).min(1, '전문분야를 1개 이상 선택해주세요'),
  experienceYears: z.number().min(0, '경력은 0년 이상이어야 합니다'),
});

type FormValues = z.infer<typeof schema>;

// ─── verification label ──────────────────────────────────────────────────────

const VERIFICATION_LABEL: Record<VerificationStatus, { text: string; bg: string; color: string }> = {
  VERIFIED: { text: '승인 완료', bg: 'bg-[#eaf3de]', color: 'text-[#3b6d11]' },
  PENDING: { text: '인증 대기', bg: 'bg-[#fef3cd]', color: 'text-[#854f0b]' },
  REVIEWING: { text: '심사 중', bg: 'bg-[#e8f0fc]', color: 'text-[#0c447c]' },
  SUPPLEMENT_REQUESTED: { text: '보완 요청', bg: 'bg-[#fcebeb]', color: 'text-[#a32d2d]' },
  REJECTED: { text: '인증 거부', bg: 'bg-[#fcebeb]', color: 'text-[#a32d2d]' },
};

// ─── page ────────────────────────────────────────────────────────────────────

export function LawyerProfilePage() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { data: profile, isLoading } = useMyLawyerProfile();
  const [isSaving, setIsSaving] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { specializations: [], experienceYears: 0 },
  });

  const selectedSpecs = watch('specializations') ?? [];

  useEffect(() => {
    if (profile) {
      reset({
        // 이 간단 폼은 L1 domains 만 편집. 심층 수정은 ProfileEditPage 에서 처리.
        specializations: profile.domains ?? [],
        experienceYears: profile.experienceYears ?? 0,
      });
    }
  }, [profile, reset]);

  async function onSubmit(values: FormValues) {
    if (!profile) return;
    setIsSaving(true);
    try {
      // BE ProfileUpdateRequest 는 L1/L2/L3 + 인증 · bio · region 을 모두 받는다.
      // 이 간단 폼은 L1(domains) + 경력만 편집하고 나머지는 기존 값을 유지한다.
      await lawyerApi.updateMe({
        domains: values.specializations,
        subDomains: profile.subDomains ?? [],
        experienceYears: values.experienceYears,
        certifications: profile.certifications ?? [],
        tags: profile.tags ?? [],
        bio: profile.bio ?? '',
        region: profile.region ?? '',
      });
      alert('프로필이 저장되었습니다.');
    } finally {
      setIsSaving(false);
    }
  }

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  if (isLoading) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="내 프로필" />
        <div className="flex items-center justify-center flex-1">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  const verification = profile?.verificationStatus
    ? VERIFICATION_LABEL[profile.verificationStatus]
    : null;

  const nameInitial = (profile?.name ?? '?')[0];

  return (
    <div className="flex flex-col flex-1">
      <Header title="내 프로필" />

      <main className="flex-1 px-[37px] py-6 pb-10 space-y-[50px]">
        {/* ── Profile header (left-aligned, figma style) ── */}
        <div className="flex items-center gap-4">
          <div className="bg-[#e8f0fc] w-[56px] h-[56px] rounded-[28px] flex items-center justify-center flex-shrink-0">
            <span className="text-[20px] font-medium text-[#1a6de0]">{nameInitial}</span>
          </div>
          <div className="flex flex-col gap-[3px]">
            <span className="text-[17px] font-medium text-[#1a1a1a]">
              {profile?.name ?? user?.name ?? '변호사'} 변호사
            </span>
            <span className="text-[12px] text-[#adb5bd]">
              {profile?.domains && profile.domains.length > 0
                ? profile.domains.join(', ')
                : '전문분야 미설정'}
            </span>
            {verification && (
              <span className={cn('text-[10px] font-medium px-[10px] py-[3px] rounded-full self-start mt-0.5', verification.bg, verification.color)}>
                {verification.text}
              </span>
            )}
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-[50px]">
          {/* ── 기본 정보 ── */}
          <section>
            <div className="border-b border-[#e9ecef] pb-2 mb-3">
              <span className="text-[12px] font-medium text-[#1a6de0]">기본 정보</span>
            </div>
            <div className="space-y-2">
              <div className="bg-white border border-[#e9ecef] rounded-[10px] px-[13px] py-[11px]">
                <p className="text-[13px] text-[#1a1a1a]">{profile?.name ?? '—'}</p>
              </div>
              <div className="bg-white border border-[#e9ecef] rounded-[10px] px-[13px] py-[11px]">
                <p className="text-[13px] text-[#1a1a1a]">{user?.email ?? '—'}</p>
              </div>
            </div>
          </section>

          {/* ── 전문 분야 ── */}
          <section>
            <div className="border-b border-[#e9ecef] pb-2 mb-3">
              <span className="text-[12px] font-medium text-[#1a6de0]">전문 분야</span>
            </div>
            <SpecializationPicker
              value={selectedSpecs}
              onChange={(v) => setValue('specializations', v, { shouldValidate: true })}
              error={errors.specializations?.message}
            />
          </section>

          {/* ── 경력 ── */}
          <section>
            <div className="border-b border-[#e9ecef] pb-2 mb-3">
              <span className="text-[12px] font-medium text-[#1a6de0]">경력</span>
            </div>
            <div className="flex items-center gap-[10px]">
              <input
                type="number"
                min={0}
                className={cn(
                  'w-[80px] bg-white border rounded-[10px] px-[13px] py-[11px] text-[13px] text-[#1a1a1a]',
                  'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
                  errors.experienceYears ? 'border-red-500' : 'border-[#e9ecef]',
                )}
                {...register('experienceYears', { valueAsNumber: true })}
              />
              <span className="text-[14px] text-[#6b7280]">년</span>
            </div>
            {errors.experienceYears && (
              <p className="text-xs text-red-500 mt-1">{errors.experienceYears.message}</p>
            )}
          </section>

          {/* ── 소개 ── */}
          <section>
            <div className="border-b border-[#e9ecef] pb-2 mb-3">
              <span className="text-[12px] font-medium text-[#1a6de0]">소개</span>
            </div>
            <div className="bg-white border border-[#e9ecef] rounded-[10px] px-[13px] py-[10px]">
              <p className="text-[13px] text-[#1a1a1a] leading-[20.8px] whitespace-pre-wrap min-h-[60px]">
                {profile?.bio || '소개를 입력해주세요.'}
              </p>
            </div>
          </section>

          {/* ── 자격 증명 서류 ── */}
          <section>
            <div className="border-b border-[#e9ecef] pb-2 mb-3">
              <span className="text-[12px] font-medium text-[#1a6de0]">자격 증명 서류</span>
            </div>
            {profile?.certifications && profile.certifications.length > 0 ? (
              <div className="space-y-2">
                {profile.certifications.map((cert) => (
                  <div key={cert} className="flex items-center gap-3">
                    <div className="bg-[#e8f0fc] text-[#0c447c] text-[10px] font-medium px-2 py-1 rounded">
                      PDF
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[12px] font-medium text-[#1a1a1a] truncate">{cert}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => navigate('/lawyer/documents')}
                      className="bg-[#f7f8fa] border border-[#e9ecef] rounded-full px-[11px] py-[6px] text-[11px] font-medium text-[#6b7280]"
                    >
                      재업로드
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <button
                type="button"
                onClick={() => navigate('/lawyer/documents')}
                className="w-full text-center py-4 text-[12px] text-[#1a6de0] border border-dashed border-[#b5d4f4] rounded-[10px] bg-[#f7f8fa]"
              >
                서류 업로드
              </button>
            )}
          </section>

          {/* ── Save button ── */}
          <button
            type="submit"
            disabled={isSaving}
            className={cn(
              'w-full bg-[#1a6de0] text-white text-[15px] font-medium py-[16px] rounded-full',
              'transition-colors hover:bg-[#1558b8] active:bg-[#104a9e]',
              isSaving && 'opacity-50 cursor-not-allowed',
            )}
          >
            {isSaving ? '저장 중...' : '저장하기'}
          </button>
        </form>

        {/* ── Quick links ── */}
        <div className="space-y-0 border-t border-[#e9ecef]">
          {[
            { label: '인증 관리', icon: Shield, to: '/lawyer/verification' },
            { label: '서류 관리', icon: FileText, to: '/lawyer/documents' },
          ].map((item) => (
            <button
              key={item.label}
              type="button"
              onClick={() => navigate(item.to)}
              className="flex w-full items-center justify-between py-4 text-left border-b border-[#e9ecef] hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-3">
                <item.icon size={18} className="text-[#adb5bd]" />
                <span className="text-[13px] text-[#1a1a1a]">{item.label}</span>
              </div>
              <ChevronRight size={16} className="text-[#adb5bd]" />
            </button>
          ))}
        </div>

        {/* ── Logout ── */}
        <Button
          variant="danger"
          fullWidth
          leftIcon={<LogOut className="h-4 w-4" />}
          onClick={handleLogout}
        >
          로그아웃
        </Button>
      </main>
    </div>
  );
}
