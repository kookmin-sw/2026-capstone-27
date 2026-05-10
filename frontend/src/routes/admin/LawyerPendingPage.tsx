import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Search } from 'lucide-react';
import { usePendingLawyers } from '@/hooks/useAdmin';
import { Spinner } from '@/components/ui';
import type { PendingLawyerResponse } from '@/types/admin';
import { cn } from '@/lib/cn';

const STATUS_BADGE: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: 'bg-[#f1f0e8]', text: 'text-[#5f5e5a]', label: '승인 대기' },
  REVIEWING: { bg: 'bg-[#e8f0fc]', text: 'text-[#0c5fa5]', label: '검토 중' },
  SUPPLEMENT_REQUESTED: { bg: 'bg-[#faeeda]', text: 'text-[#854f0b]', label: '보완 요청' },
  VERIFIED: { bg: 'bg-[#eaf3de]', text: 'text-[#3b6e11]', label: '승인 완료' },
  REJECTED: { bg: 'bg-[#fcebeb]', text: 'text-[#a32c2c]', label: '거절' },
};

const AVATAR_COLORS = ['#1a6de0', '#3b6e11', '#854f0b'];

const FILTER_TABS = [
  { value: '', label: '전체' },
  { value: 'PENDING', label: '승인 대기' },
  { value: 'REVIEWING', label: '검토 중' },
  { value: 'SUPPLEMENT_REQUESTED', label: '보완 요청' },
  { value: 'VERIFIED', label: '승인 완료' },
] as const;

export function LawyerPendingPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const { data, isLoading } = usePendingLawyers(
    page,
    20,
    search || undefined,
    statusFilter || undefined,
  );

  const lawyers: PendingLawyerResponse[] = data?.content ?? [];

  if (isLoading) return <Spinner size="lg" text="변호사 목록 불러오는 중..." />;

  return (
    <div className="space-y-4">
      {/* 검색바 */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#adb5b8]" />
        <input
          type="text"
          placeholder="🔍  이름, 이메일, 연락처로 검색"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          className="w-full h-10 pl-10 pr-4 bg-white border-[1.5px] border-[#e9edef] rounded-[20px] text-[13px] text-[#1a1a1a] placeholder:text-[#adb5b8] outline-none focus:border-[#1a6de0] transition-colors"
        />
      </div>

      {/* 필터 탭 */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar">
        {FILTER_TABS.map((tab) => (
          <button
            key={tab.value}
            type="button"
            onClick={() => { setStatusFilter(tab.value); setPage(0); }}
            className={cn(
              'h-7 px-3 rounded-[14px] text-[11px] font-normal whitespace-nowrap shrink-0 transition-colors',
              statusFilter === tab.value
                ? 'bg-[#1a6de0] text-white'
                : 'bg-white border-[1.5px] border-[#e9edef] text-[#6b7280]',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 변호사 카드 목록 */}
      <div className="space-y-3">
        {lawyers.length === 0 ? (
          <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] py-16 text-center">
            <p className="text-[13px] text-[#6b7280]">심사 대기 중인 변호사가 없습니다</p>
          </div>
        ) : (
          lawyers.map((lawyer, idx) => {
            const badge = STATUS_BADGE[lawyer.verificationStatus] ?? STATUS_BADGE.PENDING;
            const avatarColor = AVATAR_COLORS[idx % AVATAR_COLORS.length];
            const initial = lawyer.name.charAt(0);
            return (
              <div
                key={lawyer.lawyerId}
                className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] overflow-hidden"
              >
                <div className="p-3">
                  {/* 상단: 아바타 + 이름/이메일/전화 + 상태 배지 */}
                  <div className="flex items-start gap-3">
                    <div
                      className="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
                      style={{ backgroundColor: avatarColor }}
                    >
                      <span className="text-[15px] font-medium text-white">{initial}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[14px] font-medium text-[#1a1a1a]">{lawyer.name}</p>
                      <p className="text-[11px] text-[#adb5b8] mt-0.5">{lawyer.email || '-'}</p>
                      <p className="text-[11px] text-[#adb5b8]">{lawyer.phone || '-'}</p>
                    </div>
                    <span className={`${badge.bg} ${badge.text} text-[11px] font-medium h-[22px] px-3 rounded-[11px] flex items-center shrink-0`}>
                      {badge.label}
                    </span>
                  </div>

                  {/* 전문분야 태그 — L1 domains 만 표시 */}
                  {lawyer.domains && lawyer.domains.length > 0 && (
                    <div className="flex items-center gap-1.5 mt-3">
                      {lawyer.domains.map((spec: string) => (
                        <span key={spec} className="bg-[#e8f0fc] text-[#0c447c] text-[10px] h-5 px-2 rounded-[10px] flex items-center">
                          {spec}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* 경력 + 서류 수 */}
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-[11px] text-[#6b7280]">경력 {lawyer.experienceYears}년</span>
                    <span className="text-[11px] text-[#3b6e11]">서류 {lawyer.documentCount ?? 0}개</span>
                  </div>
                </div>

                {/* 구분선 + 상세 보기 */}
                <div className="border-t border-[#e9edef]">
                  <div className="p-3">
                    <Link
                      to={`/admin/lawyers/${lawyer.lawyerId}`}
                      className="bg-[#1a6de0] text-white text-[11px] font-medium h-[22px] px-3 rounded-[11px] inline-flex items-center"
                    >
                      상세 보기
                    </Link>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* 페이지네이션 */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <button
            type="button"
            disabled={data.page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 text-[11px] font-medium text-[#6b7280] bg-white border border-[#e9edef] rounded-[14px] disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-[11px] text-[#6b7280]">
            {(data.page ?? page) + 1} / {data.totalPages}
          </span>
          <button
            type="button"
            disabled={!data.hasNext}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 text-[11px] font-medium text-[#6b7280] bg-white border border-[#e9edef] rounded-[14px] disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
