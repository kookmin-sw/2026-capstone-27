import { Link, useNavigate } from 'react-router-dom';
import { Inbox, Clock, CheckCircle, XCircle } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useInboxList, useInboxStats } from '@/hooks/useInbox';
import { Badge, Card, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';
import type { InboxItemResponse } from '@/types';

// ─── helpers ────────────────────────────────────────────────────────────────

type DeliveryStatus = 'DELIVERED' | 'CONFIRMED' | 'REJECTED';
type BadgeVariant = 'primary' | 'success' | 'danger' | 'default';

const STATUS_BADGE: Record<DeliveryStatus, BadgeVariant> = {
  DELIVERED: 'primary',
  CONFIRMED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<DeliveryStatus, string> = {
  DELIVERED: '대기 중',
  CONFIRMED: '수락',
  REJECTED: '거절',
};

function statusBadgeVariant(status: string): BadgeVariant {
  return STATUS_BADGE[status as DeliveryStatus] ?? 'default';
}

function statusLabel(status: string): string {
  return STATUS_LABEL[status as DeliveryStatus] ?? status;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

// ─── stat card ───────────────────────────────────────────────────────────────

interface StatCardProps {
  label: string;
  value: number | undefined;
  icon: React.ReactNode;
  bg: string;
}

function StatCard({ label, value, icon, bg }: StatCardProps) {
  return (
    <Card padding="md" className={cn('flex items-center gap-3', bg)}>
      <div className="flex items-center justify-center w-10 h-10 rounded-full bg-white/70 flex-shrink-0">
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-xs text-gray-500 font-medium">{label}</p>
        <p className="text-2xl font-bold text-gray-900 leading-none mt-0.5">
          {value ?? '—'}
        </p>
      </div>
    </Card>
  );
}

// ─── inbox item card ──────────────────────────────────────────────────────────

function InboxItemCard({ item }: { item: InboxItemResponse }) {
  const navigate = useNavigate();
  return (
    <button
      type="button"
      onClick={() => navigate(`/lawyer/inbox/${item.deliveryId}`)}
      className="w-full text-left"
    >
      <Card
        padding="md"
        className="hover:shadow-md active:scale-[0.99] transition-all duration-150 cursor-pointer"
      >
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-gray-900 truncate">{item.title}</p>
            <div className="flex items-center gap-2 mt-1.5">
              <Badge variant="default" size="sm">
                {DOMAIN_LABELS[item.legalField] ?? item.legalField}
              </Badge>
              <Badge variant={statusBadgeVariant(item.status)} size="sm">
                {statusLabel(item.status)}
              </Badge>
            </div>
          </div>
          <span className="text-xs text-gray-400 flex-shrink-0 mt-0.5">
            {formatDate(item.createdAt)}
          </span>
        </div>
      </Card>
    </button>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function DashboardPage() {
  const { data: stats, isLoading: statsLoading } = useInboxStats();
  const { data: inboxPage, isLoading: inboxLoading } = useInboxList(0, 5);

  const isLoading = statsLoading || inboxLoading;
  const recentItems = inboxPage?.content ?? [];

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="대시보드" />

      <main className="flex-1 px-4 py-4 space-y-6 pb-10">
        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        ) : (
          <>
            {/* ── Stats 2×2 grid ─────────────────────────────────────────── */}
            <section>
              <h2 className="text-sm font-semibold text-gray-500 mb-3">현황 요약</h2>
              <div className="grid grid-cols-2 gap-3">
                <StatCard
                  label="전체 수신"
                  value={stats?.total}
                  icon={<Inbox size={20} className="text-blue-500" />}
                  bg="bg-blue-50"
                />
                <StatCard
                  label="대기 중"
                  value={stats?.pending}
                  icon={<Clock size={20} className="text-yellow-500" />}
                  bg="bg-yellow-50"
                />
                <StatCard
                  label="수락"
                  value={stats?.accepted}
                  icon={<CheckCircle size={20} className="text-green-500" />}
                  bg="bg-green-50"
                />
                <StatCard
                  label="거절"
                  value={stats?.rejected}
                  icon={<XCircle size={20} className="text-red-500" />}
                  bg="bg-red-50"
                />
              </div>
            </section>

            {/* ── Recent inbox items ─────────────────────────────────────── */}
            <section>
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-sm font-semibold text-gray-500">최근 수신</h2>
                <Link
                  to="/lawyer/inbox"
                  className="text-xs font-medium text-brand hover:text-blue-700 transition-colors"
                >
                  수신함 전체 보기 →
                </Link>
              </div>

              {recentItems.length === 0 ? (
                <Card padding="md" className="text-center py-8">
                  <p className="text-sm text-gray-400">수신된 의뢰서가 없습니다</p>
                </Card>
              ) : (
                <div className="flex flex-col gap-2">
                  {recentItems.map((item) => (
                    <InboxItemCard key={item.deliveryId} item={item} />
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
