import { useNavigate } from 'react-router-dom';
import { PlusCircle, MessageSquare } from 'lucide-react';
import { cn } from '@/lib/cn';
import { relativeTime } from '@/lib/dateUtils';
import { useConsultationList } from '@/hooks/useConsultation';
import { Button, Card, Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS, CONSULTATION_STATUS_LABELS, CONSULT_STATUS_BADGE } from '@/lib/constants';

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
    <div className="flex flex-col flex-1">
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
                      variant={CONSULT_STATUS_BADGE[c.status]}
                      size="sm"
                    >
                      {CONSULTATION_STATUS_LABELS[c.status] ?? c.status}
                    </Badge>
                    <span className="text-xs text-gray-400">
                      {relativeTime(c.lastMessageAt ?? c.createdAt)}
                    </span>
                  </div>

                  {/* Row 2: domain tags — 사용자 입력 우선, 없으면 AI 분류값 */}
                  {(() => {
                    const domains = c.userDomains ?? c.aiDomains ?? [];
                    if (domains.length === 0) return null;
                    return (
                      <div className="flex flex-wrap gap-1 mb-2">
                        {domains.map((field) => (
                          <span
                            key={field}
                            className="inline-flex items-center px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 text-xs font-medium"
                          >
                            {DOMAIN_LABELS[field] ?? field}
                          </span>
                        ))}
                      </div>
                    );
                  })()}

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
