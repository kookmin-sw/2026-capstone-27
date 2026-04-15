import { useNavigate } from 'react-router-dom';
import { PlusCircle, MessageSquare } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useConsultationList } from '@/hooks/useConsultation';
import { Button, Card, Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS, CONSULTATION_STATUS_LABELS } from '@/lib/constants';
import type { ConsultationStatus } from '@/types/enums';

// ─── helpers ────────────────────────────────────────────────────────────────

function relativeTime(iso: string | null): string {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const secs = Math.floor(diff / 1000);
  if (secs < 60) return '방금 전';
  const mins = Math.floor(secs / 60);
  if (mins < 60) return `${mins}분 전`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}일 전`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months}개월 전`;
  return `${Math.floor(months / 12)}년 전`;
}

type BadgeVariant = 'primary' | 'warning' | 'success' | 'danger' | 'default';

const STATUS_BADGE_VARIANT: Record<ConsultationStatus, BadgeVariant> = {
  COLLECTING: 'primary',
  ANALYZING: 'warning',
  AWAITING_CONFIRM: 'success',
  CONFIRMED: 'success',
  REJECTED: 'danger',
};

// ─── page ────────────────────────────────────────────────────────────────────

export function ConsultationListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useConsultationList();

  const consultations = data?.content ?? [];

  const rightAction = (
    <button
      type="button"
      onClick={() => navigate('/consultations/new')}
      aria-label="새 상담 시작"
      className={cn(
        'flex items-center gap-1 text-sm font-medium text-brand',
        'hover:text-blue-700 active:text-blue-800 transition-colors',
        '-mr-1 px-1 py-1',
      )}
    >
      <PlusCircle size={16} />
      <span>새 상담</span>
    </button>
  );

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="내 상담" rightAction={rightAction} />

      <main className="flex-1 px-4 py-4">
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        )}

        {/* Empty state */}
        {!isLoading && consultations.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-5 pt-20 pb-8 text-center">
            {/* Illustration placeholder */}
            <div className="w-24 h-24 rounded-full bg-blue-50 flex items-center justify-center">
              <MessageSquare size={40} className="text-blue-300" />
            </div>
            <div className="space-y-1.5">
              <p className="text-base font-semibold text-gray-800">
                아직 상담이 없습니다
              </p>
              <p className="text-sm text-gray-500">
                법률 전문가의 도움이 필요하신가요?
              </p>
            </div>
            <Button
              variant="primary"
              size="md"
              leftIcon={<PlusCircle size={16} />}
              onClick={() => navigate('/consultations/new')}
            >
              새 상담을 시작해보세요
            </Button>
          </div>
        )}

        {/* List */}
        {!isLoading && consultations.length > 0 && (
          <ul className="flex flex-col gap-3">
            {consultations.map((c) => (
              <li key={c.consultationId}>
                <Card
                  padding="sm"
                  className="cursor-pointer hover:shadow-md active:scale-[0.99] transition-all duration-150"
                  onClick={() => navigate(`/consultations/${c.consultationId}`)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ')
                      navigate(`/consultations/${c.consultationId}`);
                  }}
                >
                  {/* Row 1: status badge + time */}
                  <div className="flex items-center justify-between mb-2">
                    <Badge
                      variant={STATUS_BADGE_VARIANT[c.status]}
                      size="sm"
                    >
                      {CONSULTATION_STATUS_LABELS[c.status] ?? c.status}
                    </Badge>
                    <span className="text-xs text-gray-400">
                      {relativeTime(c.lastMessageAt ?? c.createdAt)}
                    </span>
                  </div>

                  {/* Row 2: domain tags */}
                  {c.primaryField && c.primaryField.length > 0 && (
                    <div className="flex flex-wrap gap-1 mb-2">
                      {c.primaryField.map((field) => (
                        <span
                          key={field}
                          className="inline-flex items-center px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 text-xs font-medium"
                        >
                          {DOMAIN_LABELS[field] ?? field}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* Row 3: last message preview */}
                  {c.lastMessage ? (
                    <p className="text-sm text-gray-700 leading-snug line-clamp-2">
                      {c.lastMessage}
                    </p>
                  ) : (
                    <p className="text-sm text-gray-400 italic">
                      아직 메시지가 없습니다
                    </p>
                  )}
                </Card>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
