import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useLawyerList } from '@/hooks/useLawyer';
import { Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';
import type { LawyerResponse } from '@/types';

// ─── filter chips ────────────────────────────────────────────────────────────

const SPECIALIZATION_FILTERS: { label: string; value: string | undefined }[] = [
  { label: '전체', value: undefined },
  { label: '민사', value: 'CIVIL' },
  { label: '형사', value: 'CRIMINAL' },
  { label: '노동', value: 'LABOR' },
  { label: '학교폭력', value: 'SCHOOL_VIOLENCE' },
];

// ─── lawyer card ─────────────────────────────────────────────────────────────

interface LawyerCardProps {
  lawyer: LawyerResponse;
  onClick: () => void;
}

function LawyerCard({ lawyer, onClick }: LawyerCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'w-full text-left bg-white rounded-card border border-[#e9edef] p-4',
        'hover:shadow-md active:scale-[0.99] transition-all duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
      )}
    >
      <div className="flex items-center gap-3">
        {/* Avatar */}
        <div className="shrink-0 w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center overflow-hidden">
          {lawyer.profileImageUrl ? (
            <img src={lawyer.profileImageUrl} alt={lawyer.name} className="w-full h-full object-cover" />
          ) : (
            <User size={22} className="text-gray-400" aria-hidden="true" />
          )}
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1">
            <span className="text-base font-medium text-[#1a1a1a]">{lawyer.name}</span>
            <ChevronRight size={16} className="text-gray-400" />
          </div>
          <p className="text-[11px] text-[#6b7280] mt-0.5">
            경력 {lawyer.experienceYears}년
          </p>
        </div>
      </div>

      {/* Specializations */}
      {lawyer.specializations && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          <span className="text-[10px] text-[#0c447c] bg-[#e8f0fc] rounded-full px-2.5 py-0.5">
            {DOMAIN_LABELS[lawyer.specializations] ?? lawyer.specializations}
          </span>
        </div>
      )}

      {/* Bio */}
      {lawyer.bio && (
        <p className="mt-2 text-xs text-[#6b7280] leading-relaxed line-clamp-2">
          {lawyer.bio}
        </p>
      )}
    </button>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function LawyerListPage() {
  const navigate = useNavigate();
  const [selectedSpecialization, setSelectedSpecialization] = useState<
    string | undefined
  >(undefined);

  const { data, isLoading } = useLawyerList(0, 20, selectedSpecialization);
  const lawyers = data?.content ?? (Array.isArray(data) ? data : []);

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="변호사 찾기" />

      {/* Filter chips */}
      <div className="sticky top-14 z-20 bg-surface border-b border-gray-100">
        <div className="flex gap-2 overflow-x-auto px-4 py-3 scrollbar-none">
          {SPECIALIZATION_FILTERS.map((filter) => {
            const isActive = selectedSpecialization === filter.value;
            return (
              <button
                key={filter.label}
                type="button"
                onClick={() => setSelectedSpecialization(filter.value)}
                className={cn(
                  'flex-shrink-0 px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-150',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
                  isActive
                    ? 'bg-brand text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200 active:bg-gray-300',
                )}
              >
                {filter.label}
              </button>
            );
          })}
        </div>
      </div>

      <main className="flex-1 px-4 py-4">
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        )}

        {/* Empty state */}
        {!isLoading && lawyers.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-4 pt-20 pb-8 text-center">
            <div className="w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center">
              <User size={36} className="text-gray-300" aria-hidden="true" />
            </div>
            <p className="text-base font-medium text-gray-500">
              조건에 맞는 변호사가 없습니다
            </p>
          </div>
        )}

        {/* Grid */}
        {!isLoading && lawyers.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {lawyers.map((lawyer: LawyerResponse) => (
              <LawyerCard
                key={lawyer.lawyerId}
                lawyer={lawyer}
                onClick={() => navigate(`/lawyers/${lawyer.lawyerId}`)}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
