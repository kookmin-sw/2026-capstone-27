import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Scale, Shield, Briefcase, Users } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useCreateConsultation } from '@/hooks/useConsultation';
import { useLegalFields } from '@/hooks/useLegalFields';
import { Button, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { DomainType } from '@/types/enums';

// ─── domain icon mapping ─────────────────────────────────────────────────────

const DOMAIN_ICONS: Record<string, React.ElementType> = {
  CIVIL: Scale,
  CRIMINAL: Shield,
  LABOR: Briefcase,
  SCHOOL_VIOLENCE: Users,
};

const DOMAIN_ENGLISH: Record<string, string> = {
  CIVIL: 'Civil',
  CRIMINAL: 'Criminal',
  LABOR: 'Labor',
  SCHOOL_VIOLENCE: 'School Violence',
};

const DOMAIN_DESCRIPTIONS: Record<string, string> = {
  CIVIL: '손해배상, 대여금, 부동산 등 개인 간의 분쟁',
  CRIMINAL: '사기, 절도, 폭행 등 범죄 관련 처벌 절차',
  LABOR: '부당해고, 임금체불 등 직장 내 권리 구제',
  SCHOOL_VIOLENCE: '학폭 심의, 징계처분 등 교육 현장 법적 대응',
};

// ─── page ────────────────────────────────────────────────────────────────────

export function NewConsultationPage() {
  const navigate = useNavigate();
  const [selected, setSelected] = useState<DomainType | null | 'UNKNOWN'>(null);
  const { mutate: createConsultation, isPending } = useCreateConsultation();
  const { data: legalFields, isLoading: fieldsLoading } = useLegalFields();

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
    <div className="flex flex-col flex-1">
      <Header
        title="분야 선택"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 flex flex-col px-5 py-6 gap-6">
        {/* Step label + title */}
        <div>
          <p className="text-xs font-bold text-brand uppercase tracking-widest mb-2">
            Step 02. Classification
          </p>
          <h2 className="text-xl font-bold text-[#181b20] leading-8">
            어떤 법률 분야에 해당하시나요?
          </h2>
          <p className="mt-2 text-sm text-[#555d6d] leading-relaxed">
            AI 분석을 보완하기 위해 정확한 분야를 직접 선택해 주세요.
            <br />
            <span className="font-semibold text-brand">주요 분야 1개</span>는 필수입니다.
          </p>
        </div>

        {/* Domain grid */}
        {fieldsLoading ? (
          <div className="flex items-center justify-center h-32">
            <Spinner size="md" />
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-2">
            {(legalFields ?? []).map(({ value, label }) => {
              const isSelected = selected === value;
              const Icon = DOMAIN_ICONS[value] ?? Scale;
              const description = DOMAIN_DESCRIPTIONS[value] ?? label;
              return (
                <button
                  key={value}
                  type="button"
                  onClick={() => setSelected(value as DomainType)}
                  className={cn(
                    'flex flex-col items-center bg-white rounded-[10px] border-2 pt-5 pb-3 px-3',
                    'shadow-[0px_2px_4px_0px_rgba(23,25,28,0.08)]',
                    'transition-all duration-150 cursor-pointer',
                    'hover:border-brand hover:shadow-md',
                    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
                    'active:scale-[0.98]',
                    isSelected
                      ? 'border-brand bg-blue-50'
                      : 'border-[#dee1e6]',
                  )}
                  aria-pressed={isSelected}
                >
                  <div
                    className={cn(
                      'mb-3 w-12 h-12 rounded-full flex items-center justify-center',
                      isSelected ? 'bg-brand text-white' : 'bg-[#f3f4f6] text-gray-500',
                      'transition-colors duration-150',
                    )}
                  >
                    <Icon size={20} />
                  </div>
                  <p
                    className={cn(
                      'text-sm font-bold mb-0.5 text-center',
                      isSelected ? 'text-brand' : 'text-gray-900',
                    )}
                  >
                    {label} ({DOMAIN_ENGLISH[value] ?? value})
                  </p>
                  <p className="text-[10px] text-[#555d6d] text-center leading-snug">
                    {description}
                  </p>
                </button>
              );
            })}
          </div>
        )}

        {/* "잘 모르겠어요" option */}
        <div className="flex justify-center">
          <button
            type="button"
            onClick={() => setSelected('UNKNOWN')}
            className={cn(
              'text-xs font-medium text-brand transition-colors duration-150',
              'focus-visible:outline-none',
              selected === 'UNKNOWN' && 'underline',
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
