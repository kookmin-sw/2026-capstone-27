import { useNavigate, useParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import { formatDateTime } from '@/lib/dateUtils';
import { useDeliveries } from '@/hooks/useBrief';
import { Button, Card, Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DELIVERY_STATUS_BADGE, DELIVERY_STATUS_LABEL } from '@/lib/constants';
import type { DeliveryStatus } from '@/types/enums';

// ─── page ────────────────────────────────────────────────────────────────────

export function BriefDeliveryPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: deliveries, isLoading } = useDeliveries(id);

  const list = deliveries ?? [];

  return (
    <div className="flex flex-col flex-1">
      <Header
        title="전달 현황"
        showBack
        onBack={() => navigate(`/briefs/${id}`)}
      />

      <main className="flex-1 px-4 py-4 flex flex-col gap-4">
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        )}

        {/* Empty state */}
        {!isLoading && list.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-4 pt-20 pb-8 text-center">
            <div className="w-20 h-20 rounded-full bg-blue-50 flex items-center justify-center">
              <Search size={32} className="text-blue-300" aria-hidden="true" />
            </div>
            <p className="text-sm text-gray-500">아직 전달 내역이 없습니다</p>
          </div>
        )}

        {/* Delivery list */}
        {!isLoading && list.length > 0 && (
          <ul className="flex flex-col gap-3">
            {list.map((d) => {
              const status = d.status as DeliveryStatus;
              return (
                <li key={d.deliveryId}>
                  <Card padding="md">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-gray-900">{d.lawyerName}</p>
                        <p className="text-xs text-gray-400 mt-0.5">{formatDateTime(d.sentAt)}</p>
                      </div>
                      <Badge
                        variant={DELIVERY_STATUS_BADGE[status] ?? 'default'}
                        size="sm"
                      >
                        {DELIVERY_STATUS_LABEL[status] ?? status}
                      </Badge>
                    </div>
                  </Card>
                </li>
              );
            })}
          </ul>
        )}

        {/* Bottom action */}
        <div className="mt-auto pt-4">
          <Button
            variant="secondary"
            fullWidth
            onClick={() => navigate('/lawyers')}
          >
            변호사 더 찾기
          </Button>
        </div>
      </main>
    </div>
  );
}
