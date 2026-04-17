import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronRight, FileText, MessageSquare } from 'lucide-react';
import { cn } from '@/lib/cn';
import { formatDate, relativeTime } from '@/lib/dateUtils';
import { useAuthStore } from '@/stores/authStore';
import { useConsultationList } from '@/hooks/useConsultation';
import { useBriefList } from '@/hooks/useBrief';
import { Card, Badge } from '@/components/ui';
import { DOMAIN_LABELS, CONSULTATION_STATUS_LABELS, BRIEF_STATUS_LABELS, CONSULT_STATUS_BADGE, BRIEF_STATUS_BADGE } from '@/lib/constants';

// ─── page ────────────────────────────────────────────────────────────────────

export function HomePage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  const { data: consultData, isLoading: consultLoading } = useConsultationList(0, 3);
  const { data: briefData, isLoading: briefLoading } = useBriefList(0, 3);

  const consultations = consultData?.content ?? [];
  const briefs = briefData?.content ?? [];

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      {/* Top bar */}
      <header className="sticky top-0 z-30 h-14 bg-white border-b border-gray-200 flex items-center px-5 safe-area-top">
        <span className="text-base font-bold text-brand tracking-tight">SHIELD</span>
      </header>

      <main className="flex-1 px-4 py-5 space-y-6">
        {/* Welcome section */}
        <section>
          <p className="text-xl font-bold text-gray-900 leading-snug">
            안녕하세요!{user?.name ? ` ${user.name}님` : ''}
          </p>
          <p className="mt-1 text-sm text-gray-500">
            오늘도 법률 도움이 필요하신가요?
          </p>
        </section>

        {/* Quick action */}
        <button
          type="button"
          onClick={() => navigate('/consultations/new')}
          className={cn(
            'w-full flex items-center justify-between',
            'bg-brand text-white rounded-2xl px-5 py-4',
            'hover:bg-blue-600 active:bg-blue-700 transition-colors duration-150',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-brand/60',
          )}
        >
          <div className="flex flex-col items-start gap-0.5">
            <span className="text-sm font-medium text-blue-100">법률 상담</span>
            <span className="text-base font-bold leading-snug">
              새로운 법률 상담 시작하기
            </span>
          </div>
          <ArrowRight size={22} className="flex-shrink-0" aria-hidden="true" />
        </button>

        {/* Recent consultations */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-700">최근 상담</h2>
            <Link
              to="/consultations"
              className="flex items-center gap-0.5 text-xs text-brand hover:text-blue-700 font-medium transition-colors"
            >
              전체 보기
              <ChevronRight size={14} aria-hidden="true" />
            </Link>
          </div>

          {consultLoading && (
            <div className="flex flex-col gap-2">
              {[1, 2].map((i) => (
                <div key={i} className="h-20 rounded-card bg-gray-100 animate-pulse" />
              ))}
            </div>
          )}

          {!consultLoading && consultations.length === 0 && (
            <Card padding="md" className="flex flex-col items-center gap-2 py-6">
              <MessageSquare size={28} className="text-gray-300" aria-hidden="true" />
              <p className="text-sm text-gray-400">아직 상담이 없습니다</p>
            </Card>
          )}

          {!consultLoading && consultations.length > 0 && (
            <ul className="flex flex-col gap-2">
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
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-1.5">
                        <Badge variant={CONSULT_STATUS_BADGE[c.status]} size="sm">
                          {CONSULTATION_STATUS_LABELS[c.status] ?? c.status}
                        </Badge>
                        {c.primaryField && c.primaryField.length > 0 && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 text-xs font-medium">
                            {DOMAIN_LABELS[c.primaryField[0]] ?? c.primaryField[0]}
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-gray-400">
                        {relativeTime(c.lastMessageAt ?? c.createdAt)}
                      </span>
                    </div>
                    {c.lastMessage ? (
                      <p className="text-sm text-gray-700 line-clamp-1">{c.lastMessage}</p>
                    ) : (
                      <p className="text-sm text-gray-400 italic">아직 메시지가 없습니다</p>
                    )}
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Recent briefs */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-700">최근 의뢰서</h2>
            <Link
              to="/briefs"
              className="flex items-center gap-0.5 text-xs text-brand hover:text-blue-700 font-medium transition-colors"
            >
              전체 보기
              <ChevronRight size={14} aria-hidden="true" />
            </Link>
          </div>

          {briefLoading && (
            <div className="flex flex-col gap-2">
              {[1, 2].map((i) => (
                <div key={i} className="h-16 rounded-card bg-gray-100 animate-pulse" />
              ))}
            </div>
          )}

          {!briefLoading && briefs.length === 0 && (
            <Card padding="md" className="flex flex-col items-center gap-2 py-6">
              <FileText size={28} className="text-gray-300" aria-hidden="true" />
              <p className="text-sm text-gray-400">아직 의뢰서가 없습니다</p>
            </Card>
          )}

          {!briefLoading && briefs.length > 0 && (
            <ul className="flex flex-col gap-2">
              {briefs.map((b) => (
                <li key={b.briefId}>
                  <Card
                    padding="sm"
                    className="cursor-pointer hover:shadow-md active:scale-[0.99] transition-all duration-150"
                    onClick={() => navigate(`/briefs/${b.briefId}`)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ')
                        navigate(`/briefs/${b.briefId}`);
                    }}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 min-w-0">
                        <Badge variant={BRIEF_STATUS_BADGE[b.status]} size="sm">
                          {BRIEF_STATUS_LABELS[b.status] ?? b.status}
                        </Badge>
                        <p className="text-sm font-medium text-gray-800 truncate">{b.title}</p>
                      </div>
                      <span className="text-xs text-gray-400 flex-shrink-0 ml-2">
                        {formatDate(b.createdAt)}
                      </span>
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
