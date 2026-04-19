import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, CheckCircle } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useLawyerList } from '@/hooks/useLawyer';
import { Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';
import type { LawyerResponse } from '@/types';

// ─── lawyer card ─────────────────────────────────────────────────────────────

interface LawyerCardProps {
  lawyer: LawyerResponse;
  onClick: () => void;
}

function LawyerCard({ lawyer, onClick }: LawyerCardProps) {
  const isVerified = lawyer.verificationStatus === 'VERIFIED';

  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'w-full text-left bg-white rounded-card shadow-sm p-4',
        'hover:shadow-md active:scale-[0.99] transition-all duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40',
      )}
    >
      {/* Profile image + info */}
      <div className="flex items-start gap-3">
        {/* Avatar */}
        <div className="flex-shrink-0 w-14 h-14 rounded-full bg-gray-100 flex items-center justify-center">
          {lawyer.profileImageUrl ? (
            <img
              src={lawyer.profileImageUrl}
              alt={lawyer.name}
              className="w-14 h-14 rounded-full object-cover"
            />
          ) : (
            <User size={26} className="text-gray-400" aria-hidden="true" />
          )}
        </div>

        {/* Name + verification */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-base font-semibold text-gray-900 truncate">
              {lawyer.name}
            </span>
            {isVerified ? (
              <span className="inline-flex items-center gap-1 text-xs font-medium text-green-600 bg-green-50 px-2 py-0.5 rounded-full">
                <CheckCircle size={11} aria-hidden="true" />
                인증됨
              </span>
            ) : (
              <span className="inline-flex items-center text-xs font-medium text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">
                심사 중
              </span>
            )}
          </div>

          {/* Experience */}
          <p className="mt-0.5 text-sm text-gray-500">
            {lawyer.experienceYears}년 경력
          </p>
        </div>
      </div>

      {/* Bio excerpt */}
      {lawyer.bio && (
        <p className="mt-2.5 text-sm text-gray-500 leading-relaxed line-clamp-2">
          {lawyer.bio}
        </p>
      )}

      {/* Specialization badge */}
      {lawyer.specializations && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          <Badge variant="primary" size="sm">
            {DOMAIN_LABELS[lawyer.specializations] ?? lawyer.specializations}
          </Badge>
        </div>
      )}

      {/* View profile CTA */}
      <div className="mt-3 pt-3 border-t border-gray-100">
        <span className="text-sm font-medium text-brand">프로필 보기 →</span>
      </div>
    </button>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function LawyerListPage() {
  const navigate = useNavigate();
  const [selectedSpecialization, setSelectedSpecialization] = useState<
    string | undefined
  >(undefined);

  const filters: { label: string; value: string | undefined }[] = [
    { label: '전체', value: undefined },
    ...Object.entries(DOMAIN_LABELS).map(([value, label]) => ({
      label,
      value,
    })),
  ];

  const { data, isLoading } = useLawyerList(0, 20, selectedSpecialization);
  const lawyers = data?.content ?? (Array.isArray(data) ? data : []);

  return (
    <div className="flex flex-col flex-1">
      <Header title="변호사 찾기" />

      {/* Filter chips */}
      <div className="sticky top-14 z-20 bg-surface border-b border-gray-100">
        <div className="flex gap-2 overflow-x-auto px-4 py-3 scrollbar-none">
          {filters.map((filter) => {
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
