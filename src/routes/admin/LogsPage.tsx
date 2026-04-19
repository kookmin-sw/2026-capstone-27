import { useState } from 'react';
import { useVerificationLogs } from '@/hooks/useAdmin';
import { formatDate } from '@/lib/dateUtils';
import { Spinner } from '@/components/ui';
import { FileText } from 'lucide-react';
import { cn } from '@/lib/cn';

const STATUS_BADGE: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: 'bg-[#f1f0e8]', text: 'text-[#5f5e5a]', label: '승인 대기' },
  REVIEWING: { bg: 'bg-[#e8f0fc]', text: 'text-[#0c5fa5]', label: '검토 중' },
  SUPPLEMENT_REQUESTED: { bg: 'bg-[#faeeda]', text: 'text-[#854f0b]', label: '보완 요청' },
  VERIFIED: { bg: 'bg-[#eaf3de]', text: 'text-[#3b6e11]', label: '승인 완료' },
  REJECTED: { bg: 'bg-[#fcebeb]', text: 'text-[#a32c2c]', label: '거절' },
};

/**
 * BE 는 VerificationStatus.name() 을 fromStatus/toStatus 로 저장하지만
 * 예외적으로 APPROVED 같은 구식 값이 남아있을 경우 VERIFIED 로 매핑.
 */
function normalizeStatus(status: string): string {
  if (status === 'APPROVED') return 'VERIFIED';
  return status;
}

/** FILTER_TABS 는 value 가 빈 문자열(전체), period(today/week), 또는 status 값(VERIFIED/REJECTED/...). */
const FILTER_TABS = [
  { value: '', label: '전체' },
  { value: 'today', label: '오늘' },
  { value: 'week', label: '최근 7일' },
  { value: 'VERIFIED', label: '승인' },
  { value: 'REJECTED', label: '거절' },
  { value: 'SUPPLEMENT_REQUESTED', label: '보완' },
] as const;

export function LogsPage() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<(typeof FILTER_TABS)[number]['value']>('');

  // BE 는 period(today/week) / status 만 지원. 임의 날짜 범위는 지원 없음.
  const filterParams =
    filter === 'today' || filter === 'week'
      ? { period: filter as 'today' | 'week' }
      : filter
        ? { status: filter }
        : undefined;

  const { data, isLoading } = useVerificationLogs(page, 20, filterParams);

  return (
    <div className="space-y-4">
      {/* 필터 탭 */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar">
        {FILTER_TABS.map((tab) => (
          <button
            key={tab.value}
            type="button"
            onClick={() => { setFilter(tab.value); setPage(0); }}
            className={cn(
              'h-[26px] px-3 rounded-[13px] text-[12px] font-normal whitespace-nowrap shrink-0 transition-colors',
              filter === tab.value
                ? 'bg-[#1a6de0] text-white'
                : 'bg-white border-[1.5px] border-[#e9edef] text-[#6b7280]',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 이력 카드 목록 */}
      {isLoading ? (
        <div className="flex items-center justify-center h-48">
          <Spinner size="lg" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] py-16 text-center">
          <FileText className="h-12 w-12 text-[#e9edef] mx-auto mb-3" />
          <p className="text-[13px] text-[#6b7280]">처리 이력이 없습니다</p>
        </div>
      ) : (
        <div className="space-y-3.5">
          {data.content.map((log) => {
            const before = normalizeStatus(log.fromStatus);
            const after = normalizeStatus(log.toStatus);
            const beforeBadge = STATUS_BADGE[before] ?? STATUS_BADGE.PENDING;
            const afterBadge = STATUS_BADGE[after] ?? STATUS_BADGE.PENDING;

            return (
              <div
                key={log.logId}
                className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] overflow-hidden"
              >
                <div className="p-3">
                  {/* 이름 + 시각 */}
                  <div className="flex items-center justify-between">
                    <p className="text-[13px] font-medium text-[#1a1a1a]">{log.lawyerName}</p>
                    <span className="text-[11px] text-[#adb5b8]">{formatDate(log.createdAt)}</span>
                  </div>

                  {/* 상태 전이 */}
                  <div className="flex items-center gap-1.5 mt-2">
                    <span className={`${beforeBadge.bg} ${beforeBadge.text} text-[10px] font-medium h-[22px] px-2.5 rounded-[11px] flex items-center`}>
                      {beforeBadge.label}
                    </span>
                    <span className="text-[11px] text-[#adb5b8]">→</span>
                    <span className={`${afterBadge.bg} ${afterBadge.text} text-[10px] font-medium h-[22px] px-2.5 rounded-[11px] flex items-center`}>
                      {afterBadge.label}
                    </span>
                  </div>
                </div>

                {/* 구분선 */}
                <div className="h-[0.5px] bg-[#e9edef]" />

                {/* 하단: 담당자 + 사유 */}
                <div className="px-3 py-2">
                  <p className="text-[11px] text-[#adb5b8]">담당: {log.adminName || '-'}</p>
                  {log.reason && (
                    <p className="text-[10px] text-[#6b7280] mt-0.5">{log.reason}</p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* 페이지네이션 */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 text-[11px] font-medium text-[#6b7280] bg-white border border-[#e9edef] rounded-[13px] disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-[11px] text-[#6b7280]">
            {page + 1} / {data.totalPages}
          </span>
          <button
            type="button"
            disabled={!data.hasNext}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 text-[11px] font-medium text-[#6b7280] bg-white border border-[#e9edef] rounded-[13px] disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
