import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { UserCheck, ShieldCheck, UserX, CircleCheck, Circle } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useUpdateBrief } from '@/hooks/useBrief';
import { Button } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { PrivacySetting } from '@/types/enums';

// ─── types ───────────────────────────────────────────────────────────────────

type PrivacyLevel = 'full' | 'partial' | 'anonymous';

interface PrivacyOption {
  value: PrivacyLevel;
  icon: React.ReactNode;
  iconBg: string;
  title: string;
  description: string;
  previewName: string;
  previewBadge: string;
}

const OPTIONS: PrivacyOption[] = [
  {
    value: 'full',
    icon: <UserCheck size={20} className="text-[#575e6b]" />,
    iconBg: 'bg-[#f3f5f6]',
    title: '전체 공개',
    description: '본인의 실명으로 사건을 의뢰합니다. 변호사의 신뢰도가 높아집니다.',
    previewName: '홍길동',
    previewBadge: '의뢰인',
  },
  {
    value: 'partial',
    icon: <ShieldCheck size={20} className="text-white" />,
    iconBg: 'bg-brand',
    title: '부분 공개',
    description: '이름의 일부를 마스킹 처리하여 공개합니다. 보안과 신뢰의 균형을 맞춥니다.',
    previewName: '홍○동',
    previewBadge: '의뢰인',
  },
  {
    value: 'anonymous',
    icon: <UserX size={20} className="text-[#575e6b]" />,
    iconBg: 'bg-[#f3f5f6]',
    title: '비공개 (익명)',
    description: '실명을 완전히 숨기고 익명으로 의뢰합니다. 개인정보를 최우선으로 보호합니다.',
    previewName: '익명의 의뢰인',
    previewBadge: '의뢰인',
  },
];

// ─── page ────────────────────────────────────────────────────────────────────

/** UI 선택값 → API PrivacySetting 매핑 */
const PRIVACY_MAP: Record<PrivacyLevel, PrivacySetting> = {
  full: 'PUBLIC',
  partial: 'PARTIAL',
  anonymous: 'PARTIAL', // API에서 anonymous는 미지원 — PARTIAL로 대체
};

export function PrivacySettingsPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const updateBrief = useUpdateBrief(id);
  const [selected, setSelected] = useState<PrivacyLevel>('partial');

  async function handleSave() {
    try {
      await updateBrief.mutateAsync({ privacySetting: PRIVACY_MAP[selected] });
      navigate(`/briefs/${id}/review`);
    } catch {
      // 에러 시 페이지 유지
    }
  }

  return (
    <div className="flex flex-col flex-1">
      <Header
        title="개인정보 공개 설정"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 px-6 pt-4 pb-32 space-y-5 overflow-y-auto">
        {/* Title */}
        <div>
          <h2 className="text-xl font-bold text-[#161a1d] leading-8">
            변호사에게 공개할
            <br />
            본인의 신원을 선택해 주세요
          </h2>
          <p className="text-sm text-[#31383f] mt-2">
            설정한 공개 범위는 변호사와의 상담 시에만 적용됩니다.
          </p>
        </div>

        {/* Privacy option cards */}
        {OPTIONS.map((opt) => {
          const isSelected = selected === opt.value;
          return (
            <button
              key={opt.value}
              type="button"
              onClick={() => setSelected(opt.value)}
              className={cn(
                'w-full text-left rounded-card p-5 transition-all',
                isSelected
                  ? 'border-2 border-brand shadow-md bg-white'
                  : 'border-2 border-[#dde0e4] bg-[#f9fafb]',
              )}
            >
              <div className="flex items-start gap-4">
                <div className={cn('shrink-0 w-9 h-9 rounded-[10px] flex items-center justify-center mt-1', opt.iconBg)}>
                  {opt.icon}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-base font-bold text-[#161a1d]">{opt.title}</p>
                  <p className="text-sm text-[#31383f] leading-relaxed mt-1">{opt.description}</p>
                </div>
                {isSelected ? (
                  <CircleCheck size={24} className="text-brand shrink-0" />
                ) : (
                  <Circle size={24} className="text-[#dde0e4] shrink-0" />
                )}
              </div>

              {/* Preview */}
              <div className="mt-4">
                <p className="text-[11px] font-medium text-[#31383f] uppercase tracking-tight mb-2">
                  변호사 확인 화면 미리보기
                </p>
                <div className={cn(
                  'rounded-[10px] border p-4 flex items-center gap-3',
                  isSelected ? 'bg-[#f0faff] border-brand/30' : 'bg-[#f3f5f6]/30 border-[#dde0e4]',
                )}>
                  <div className="w-10 h-10 rounded-full bg-[#e1f1fd] shrink-0" />
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-[#31383f]">{opt.previewName}</span>
                      <span className="text-[10px] text-brand border border-brand/40 rounded-full px-1.5">
                        {opt.previewBadge}
                      </span>
                    </div>
                    <p className="text-[11px] text-[#31383f] mt-0.5">상담 대기 중 · 민사 사건</p>
                  </div>
                </div>
              </div>
            </button>
          );
        })}

        {/* Notice */}
        <div className="bg-[#f3f5f6]/40 border border-[#dde0e4] border-dashed rounded-card p-4">
          <p className="text-[10px] text-[#31383f] leading-5">
            • 개인정보 공개 설정은 의뢰서 제출 이후에는 변경이 불가능합니다.
            <br />
            • 정확한 법률 상담을 위해 가능하면 부분 공개 이상의 설정을 권장합니다.
          </p>
        </div>
      </main>

      {/* Fixed bottom CTA */}
      <div className="sticky bottom-0 bg-linear-to-t from-white via-white/95 to-transparent px-6 pt-10 pb-6 safe-area-bottom">
        <Button
          variant="primary"
          fullWidth
          size="lg"
          isLoading={updateBrief.isPending}
          onClick={handleSave}
          className="rounded-card h-14 shadow-lg shadow-brand/25"
        >
          다음
        </Button>
      </div>
    </div>
  );
}
