import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/cn';
import { useCreateConsultation } from '@/hooks/useConsultation';
import { Button, Card } from '@/components/ui';
import { CategoryPicker } from '@/components/ui/CategoryPicker';
import { Header } from '@/components/layout/Header';
import type { CategorySelection } from '@/lib/legalCategories';

// ─── helpers ─────────────────────────────────────────────────────────────────

/**
 * 선택된 CategorySelection[] 배열을 백엔드 3단계 분류 형태로 변환.
 *
 *  - path 길이 1 → 대분류(domains)
 *  - path 길이 2 → 중분류(subDomains) — 상위 대분류도 함께 포함
 *  - path 길이 3 → 소분류(tags) — 상위 대/중분류도 함께 포함
 *
 *  중복은 Set 으로 제거.
 */
function toClassificationRequest(selections: CategorySelection[]): {
  domains: string[];
  subDomains: string[];
  tags: string[];
} {
  const domains = new Set<string>();
  const subDomains = new Set<string>();
  const tags = new Set<string>();

  for (const sel of selections) {
    const [l1, l2, l3] = sel.path;
    if (l1) domains.add(l1);
    if (l2) subDomains.add(l2);
    if (l3) tags.add(l3);
  }

  return {
    domains: Array.from(domains),
    subDomains: Array.from(subDomains),
    tags: Array.from(tags),
  };
}

// ─── page ────────────────────────────────────────────────────────────────────

export function NewConsultationPage() {
  const navigate = useNavigate();
  const [selected, setSelected] = useState<CategorySelection[]>([]);
  const [isUnknown, setIsUnknown] = useState(false);
  const { mutate: createConsultation, isPending } = useCreateConsultation();

  const isDomainChosen = selected.length > 0 || isUnknown;

  function handleCategoryChange(value: CategorySelection[]) {
    setSelected(value);
    if (value.length > 0) setIsUnknown(false);
  }

  function handleUnknownToggle() {
    setIsUnknown(!isUnknown);
    if (!isUnknown) setSelected([]);
  }

  function handleSubmit() {
    if (!isDomainChosen) return;

    // "잘 모르겠어요" → 세 배열 모두 빈 값으로 전달.
    // 그 외에는 선택된 경로를 domains/subDomains/tags 로 분해해 전달.
    const request = isUnknown
      ? { domains: [], subDomains: [], tags: [] }
      : toClassificationRequest(selected);

    createConsultation(request, {
      onSuccess: (res) => {
        const newId = res.data.data.consultationId;
        navigate(`/consultations/${newId}`);
      },
    });
  }

  return (
    <div className="flex flex-col flex-1">
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
            관련 분야를 모두 선택해 주세요. 정확하지 않아도 괜찮습니다.
          </p>
        </Card>

        {/* Category picker */}
        <CategoryPicker
          value={selected}
          onChange={handleCategoryChange}
          placeholder="분야 검색 (예: 보증금 반환, 이혼, 해고...)"
        />

        {/* "잘 모르겠어요" option */}
        <div className="flex justify-center">
          <button
            type="button"
            onClick={handleUnknownToggle}
            className={cn(
              'text-sm font-medium transition-colors duration-150',
              'focus-visible:outline-none focus-visible:underline',
              isUnknown
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
