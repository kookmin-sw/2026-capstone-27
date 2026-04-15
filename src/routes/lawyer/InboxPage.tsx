import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/cn';
import { useInboxList } from '@/hooks/useInbox';
import { Badge, Card, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';
import type { InboxItemResponse } from '@/types';

// ─── helpers ────────────────────────────────────────────────────────────────

type FilterTab = 'ALL' | 'DELIVERED' | 'CONFIRMED' | 'REJECTED';
type BadgeVariant = 'primary' | 'success' | 'danger' | 'default';

const TABS: { key: FilterTab; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'DELIVERED', label: '대기 중' },
  { key: 'CONFIRMED', label: '수락' },
  { key: 'REJECTED', label: '거절' },
];

const STATUS_BADGE: Record<string, BadgeVariant> = {
  DELIVERED: 'primary',
  CONFIRMED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<string, string> = {
  DELIVERED: '대기 중',
  CONFIRMED: '수락',
  REJECTED: '거절',
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

// ─── item card ───────────────────────────────────────────────────────────────

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
              <Badge variant={STATUS_BADGE[item.status] ?? 'default'} size="sm">
                {STATUS_LABEL[item.status] ?? item.status}
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

export function InboxPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');

  const statusFilter = activeTab === 'ALL' ? undefined : activeTab;
  const { data: inboxPage, isLoading } = useInboxList(0, 50, statusFilter);
  const items = inboxPage?.content ?? [];

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="수신함" showBack onBack={() => navigate('/lawyer')} />

      {/* Tabs */}
      <div className="bg-white border-b border-gray-200 px-4">
        <div className="flex gap-0">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActiveTab(tab.key)}
                className={cn(
                  'px-4 py-3 text-sm font-medium transition-colors duration-150',
                  'border-b-2 -mb-px',
                  isActive
                    ? 'border-brand text-brand'
                    : 'border-transparent text-gray-500 hover:text-gray-700',
                )}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      <main className="flex-1 px-4 py-4 pb-10">
        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        ) : items.length === 0 ? (
          <div className="flex items-center justify-center h-48">
            <p className="text-sm text-gray-400">수신된 의뢰서가 없습니다</p>
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {items.map((item) => (
              <InboxItemCard key={item.deliveryId} item={item} />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
