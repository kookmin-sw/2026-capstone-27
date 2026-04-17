import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Scale, Shield, Briefcase, Users } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useCreateConsultation } from '@/hooks/useConsultation';
import { Button } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { DomainType } from '@/types/enums';

// ─── domain options ──────────────────────────────────────────────────────────

interface DomainOption {
  value: DomainType;
  label: string;
  description: string;
  Icon: React.ElementType;
}

const DOMAIN_OPTIONS: DomainOption[] = [
  {
    value: 'CIVIL',
    label: '민사 (Civil)',
    description: '손해배상, 대여금, 부동산 등 개인 간의 분쟁',
    Icon: Scale,
  },
  {
    value: 'CRIMINAL',
    label: '형사 (Criminal)',
    description: '사기, 절도, 폭행 등 범죄 관련 처벌 절차',
    Icon: Shield,
  },
  {
    value: 'LABOR',
    label: '노동법 (Labor)',
    description: '부당해고, 임금체불 등 직장 내 권리 구제',
    Icon: Briefcase,
  },
  {
    value: 'SCHOOL_VIOLENCE',
    label: '학교폭력 (School Violence)',
    description: '학폭 심의, 징계처분 등 교육 현장 법적 대응',
    Icon: Users,
  },
];

// ─── page ────────────────────────────────────────────────────────────────────

export function NewConsultationPage() {
  const navigate = useNavigate();
  const [selected, setSelected] = useState<DomainType | null | 'UNKNOWN'>(null);
  const { mutate: createConsultation, isPending } = useCreateConsultation();

  // "잘 모르겠어요" uses domain=null, but we track it as 'UNKNOWN' locally
  const isDomainChosen = selected !== null;

  function handleSubmit() {
    if (!isDomainChosen) return;
    const domain: DomainType | null =
      selected === 'UNKNOWN' ? null : (selected as DomainType);

    createConsultation(domain, {
      onSuccess: (res) => {
        const newId = res.data.data.consultationId;
        navigate(`/consultations/${newId}`);
      },
    });
  }

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header
        title="새 상담"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 flex flex-col px-5 py-6 gap-5">
        {/* Step indicator + Title */}
        <div>
          <p className="text-xs font-bold text-brand uppercase tracking-wide mb-2">
            Step 02. Classification
          </p>
          <h2 className="text-xl font-bold text-[#181b20] leading-8 mb-2">
            어떤 법률 분야에 해당하시나요?
          </h2>
          <p className="text-sm text-[#555d6d] leading-relaxed">
            AI 분석을 보완하기 위해 정확한 분야를 직접 선택해 주세요.
            <br />
            <span className="font-semibold text-brand">주요 분야 1개</span>는 필수입니다.
          </p>
        </div>

        {/* Domain grid */}
        <div className="grid grid-cols-2 gap-3">
          {DOMAIN_OPTIONS.map(({ value, label, description, Icon }) => {
            const isSelected = selected === value;
            return (
              <button
                key={value}
                type="button"
                onClick={() => setSelected(value)}
                className={cn(
                  'flex flex-col items-center text-center bg-white rounded-[10px] border-2 p-4',
                  'min-h-40 transition-all duration-150 cursor-pointer',
                  'shadow-[0px_2px_4px_0px_rgba(23,25,28,0.08)]',
                  'hover:border-brand hover:shadow-md',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
                  'active:scale-[0.98]',
                  isSelected
                    ? 'border-brand bg-brand/5'
                    : 'border-[#dee1e6]',
                )}
                aria-pressed={isSelected}
              >
                <div
                  className={cn(
                    'mb-3 w-12 h-12 rounded-full flex items-center justify-center',
                    isSelected ? 'bg-brand text-white' : 'bg-[#f3f4f6] text-[#555d6d]',
                    'transition-colors duration-150',
                  )}
                >
                  <Icon size={24} />
                </div>
                <p
                  className={cn(
                    'text-sm font-bold mb-1',
                    isSelected ? 'text-brand' : 'text-[#181b20]',
                  )}
                >
                  {label}
                </p>
                <p className="text-[10px] text-[#555d6d] leading-3.25">
                  {description}
                </p>
              </button>
            );
          })}
        </div>

        {/* "잘 모르겠어요" option */}
        <div className="flex justify-center">
          <button
            type="button"
            onClick={() => setSelected('UNKNOWN')}
            className={cn(
              'text-[10px] font-medium transition-colors duration-150',
              'focus-visible:outline-none focus-visible:underline',
              selected === 'UNKNOWN'
                ? 'text-brand underline'
                : 'text-brand/60 hover:text-brand',
            )}
          >
            잘 모르겠어요
          </button>
        </div>

        {/* Spacer */}
        <div className="flex-1" />

        {/* Submit */}
        <div className="pb-safe">
          <Button
            variant="primary"
            size="lg"
            fullWidth
            disabled={!isDomainChosen}
            isLoading={isPending}
            onClick={handleSubmit}
          >
            상담 시작
          </Button>
        </div>
      </main>
    </div>
  );
}
