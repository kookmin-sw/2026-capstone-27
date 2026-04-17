import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Scale, Shield, Briefcase, Users } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useCreateConsultation } from '@/hooks/useConsultation';
import { useLegalFields } from '@/hooks/useLegalFields';
import { Button, Card, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { DomainType } from '@/types/enums';

// ─── domain icon mapping ─────────────────────────────────────────────────────

const DOMAIN_ICONS: Record<string, React.ElementType> = {
  CIVIL: Scale,
  CRIMINAL: Shield,
  LABOR: Briefcase,
  SCHOOL_VIOLENCE: Users,
};

const DOMAIN_DESCRIPTIONS: Record<string, string> = {
  CIVIL: '계약, 손해배상, 부동산 등',
  CRIMINAL: '고소, 고발, 형사 사건 등',
  LABOR: '해고, 임금, 근로조건 등',
  SCHOOL_VIOLENCE: '학교폭력 사건 대응',
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
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header
        title="새 상담"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 flex flex-col px-4 py-6 gap-6">
        {/* Description card */}
        <Card padding="md">
          <p className="text-sm font-medium text-gray-500 mb-0.5">분야 선택</p>
          <p className="text-base font-semibold text-gray-900">
            어떤 분야의 상담이 필요하신가요?
          </p>
          <p className="mt-1.5 text-sm text-gray-500 leading-relaxed">
            가장 가까운 분야를 선택해 주세요. 정확하지 않아도 괜찮습니다.
          </p>
        </Card>

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
                    'text-left bg-white rounded-card border-2 p-4',
                    'transition-all duration-150 cursor-pointer',
                    'hover:border-brand hover:shadow-sm',
                    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
                    'active:scale-[0.98]',
                    isSelected
                      ? 'border-brand bg-blue-50 shadow-sm'
                      : 'border-gray-200',
                  )}
                  aria-pressed={isSelected}
                >
                  <div
                    className={cn(
                      'mb-3 w-10 h-10 rounded-xl flex items-center justify-center',
                      isSelected ? 'bg-brand text-white' : 'bg-gray-100 text-gray-500',
                      'transition-colors duration-150',
                    )}
                  >
                    <Icon size={20} />
                  </div>
                  <p
                    className={cn(
                      'text-sm font-semibold mb-0.5',
                      isSelected ? 'text-brand' : 'text-gray-900',
                    )}
                  >
                    {label}
                  </p>
                  <p className="text-xs text-gray-500 leading-snug">
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
              'text-sm font-medium transition-colors duration-150',
              'focus-visible:outline-none focus-visible:underline',
              selected === 'UNKNOWN'
                ? 'text-brand underline'
                : 'text-gray-400 hover:text-gray-600',
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
