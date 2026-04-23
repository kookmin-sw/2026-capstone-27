import { useNavigate } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { formatDate } from '@/lib/dateUtils';
import { useBriefList } from '@/hooks/useBrief';
import { Button, Card, Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { BRIEF_STATUS_LABELS, BRIEF_STATUS_BADGE } from '@/lib/constants';

// ─── page ────────────────────────────────────────────────────────────────────

export function BriefListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useBriefList();

  const briefs = data?.content ?? [];

  return (
    <div className="flex flex-col flex-1">
      <Header title="내 의뢰서" />

      <main className="flex-1 px-4 py-4">
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        )}

        {/* Empty state */}
        {!isLoading && briefs.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-5 pt-20 pb-8 text-center">
            <div className="w-24 h-24 rounded-full bg-blue-50 flex items-center justify-center">
              <FileText size={40} className="text-blue-300" aria-hidden="true" />
            </div>
            <div className="space-y-1.5">
              <p className="text-base font-semibold text-gray-800">아직 의뢰서가 없습니다</p>
              <p className="text-sm text-gray-500">상담을 통해 의뢰서를 작성해보세요</p>
            </div>
            <Button
              variant="primary"
              size="md"
              onClick={() => navigate('/consultations/new')}
            >
              상담 시작하기
            </Button>
          </div>
        )}

        {/* List */}
        {!isLoading && briefs.length > 0 && (
          <ul className="flex flex-col gap-3">
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
                  <div className="flex items-center justify-between gap-3">
                    {b.acceptedLawyerId ? (
                      <Badge variant="success" size="sm">담당 변호사 확정</Badge>
                    ) : (
                      <Badge variant={BRIEF_STATUS_BADGE[b.status]} size="sm">
                        {BRIEF_STATUS_LABELS[b.status] ?? b.status}
                      </Badge>
                    )}
                    <p className="flex-1 text-sm font-medium text-gray-800 truncate">
                      {b.title}
                    </p>
                    <span className="text-xs text-gray-400 flex-shrink-0">
                      {formatDate(b.createdAt)}
                    </span>
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
