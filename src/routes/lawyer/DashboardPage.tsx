import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Clock } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useInboxList, useInboxStats, useUpdateInboxStatus } from '@/hooks/useInbox';
import { useMyLawyerProfile } from '@/hooks/useLawyer';
import { Button, Modal, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS, DELIVERY_STATUS_LABEL } from '@/lib/constants';
import type { InboxItemResponse } from '@/types';

const rejectTextareaClass = cn(
  'w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-sm text-[#1E293B]',
  'placeholder:text-[#64748B] resize-none',
  'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
  'transition-colors duration-150',
);

// ─── helpers ────────────────────────────────────────────────────────────────

function formatShortDate(iso: string): string {
  const d = new Date(iso);
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  return `${m}.${day} ${h}:${min}`;
}

function getStatusBadgeStyle(status: string) {
  if (status === 'DELIVERED') return { bg: 'bg-[#e8f0fc]', text: 'text-[#0c447c]' };
  if (status === 'CONFIRMED') return { bg: 'bg-[#eaf3de]', text: 'text-[#3b6d11]' };
  if (status === 'REJECTED') return { bg: 'bg-[#fcebeb]', text: 'text-[#a32d2d]' };
  return { bg: 'bg-[#f1efe8]', text: 'text-[#5f5e5a]' };
}

// ─── stat card ───────────────────────────────────────────────────────────────

interface StatCardProps {
  label: string;
  value: number | undefined;
  color: 'accent' | 'blue' | 'brown' | 'green';
}

function StatCard({ label, value, color }: StatCardProps) {
  const isAccent = color === 'accent';
  const numColor = {
    accent: 'text-white',
    blue: 'text-[#1a6de0]',
    brown: 'text-[#854f0b]',
    green: 'text-[#3b6d11]',
  }[color];

  return (
    <div
      className={cn(
        'rounded-[14px] p-[15px] flex flex-col gap-[5px]',
        isAccent
          ? 'bg-[#1a6de0]'
          : 'bg-white border border-[#e9ecef]',
      )}
    >
      <p className={cn('text-[28px] font-bold leading-[28px]', numColor)}>
        {value ?? 0}
      </p>
      <p className={cn('text-[11px]', isAccent ? 'text-white/75' : 'text-[#6b7280]')}>
        {label}
      </p>
    </div>
  );
}

// ─── recent item (flat style) ────────────────────────────────────────────────

function RecentItem({ item }: { item: InboxItemResponse }) {
  const navigate = useNavigate();
  const statusStyle = getStatusBadgeStyle(item.status);

  return (
    <div className="py-2">
      {/* badges */}
      <div className="flex flex-wrap gap-[7px] items-center">
        <span className="bg-[#e8f0fc] text-[#0c447c] text-[11px] font-medium px-[10px] py-[3px] rounded-full">
          {DOMAIN_LABELS[item.legalField] ?? item.legalField}
        </span>
        <span className={cn('text-[11px] font-medium px-[10px] py-[3px] rounded-full', statusStyle.bg, statusStyle.text)}>
          {DELIVERY_STATUS_LABEL[item.status] ?? item.status}
        </span>
      </div>

      {/* title / description */}
      <p className="text-[12px] text-[#6b7280] leading-[19.2px] mt-2 line-clamp-2">
        {item.briefTitle}
      </p>

      {/* divider + footer */}
      <div className="border-t border-[#e9ecef] mt-3 pt-[10px] flex items-center justify-between">
        <span className="text-[11px] text-[#adb5bd]">
          전달 {formatShortDate(item.sentAt)}
        </span>
        <button
          type="button"
          onClick={() => navigate(`/lawyer/inbox/${item.deliveryId}`)}
          className="bg-[#f7f8fa] border border-[#e9ecef] rounded-full px-[11px] py-[6px] text-[11px] font-medium text-[#6b7280]"
        >
          상세 보기
        </button>
      </div>
    </div>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function DashboardPage() {
  const { data: stats, isLoading: statsLoading } = useInboxStats();
  const { data: inboxPage, isLoading: inboxLoading } = useInboxList(0, 5);
  const { data: profile } = useMyLawyerProfile();

  const isLoading = statsLoading || inboxLoading;
  const recentItems = inboxPage?.content ?? [];
  const lawyerName = profile?.name ?? '변호사';

  // Urgent request (첫 번째 DELIVERED 건) — 수락/거절 대상
  const urgentItem = recentItems.find((item) => item.status === 'DELIVERED');
  const updateStatus = useUpdateInboxStatus(urgentItem?.deliveryId ?? '');

  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function handleAccept() {
    if (!urgentItem) return;
    await updateStatus.mutateAsync({ status: 'CONFIRMED' });
    setConfirmModalOpen(false);
    setSuccessMessage('의뢰를 수락했습니다.');
    setTimeout(() => setSuccessMessage(''), 2000);
  }

  async function handleReject() {
    if (!urgentItem) return;
    await updateStatus.mutateAsync({
      status: 'REJECTED',
      rejectionReason: rejectReason || undefined,
    });
    setRejectModalOpen(false);
    setRejectReason('');
    setSuccessMessage('의뢰를 거절했습니다.');
    setTimeout(() => setSuccessMessage(''), 2000);
  }

  return (
    <div className="flex flex-col flex-1">
      <Header title="변호사 대시보드" />

      <main className="flex-1 px-[22px] py-4 space-y-4 pb-10">
        {successMessage && (
          <div className="px-4 py-3 bg-green-50 border border-green-200 rounded-xl">
            <p className="text-sm text-green-700 font-medium">{successMessage}</p>
          </div>
        )}

        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        ) : (
          <>
            {/* ── Greeting banner ────────────────────────────────────── */}
            <div className="bg-white border border-[#e9ecef] rounded-[12px] px-[13px] py-[11px]">
              <p className="text-[13px] text-[#6b7280]">
                {lawyerName}님, 오늘도 좋은 하루입니다.
              </p>
            </div>

            {/* ── Stats 2×2 grid ─────────────────────────────────────── */}
            <div className="grid grid-cols-2 gap-[10px] pt-1">
              <StatCard label="신규 의뢰" value={stats?.pending} color="accent" />
              <StatCard label="검토 중" value={stats?.total} color="blue" />
              <StatCard label="진행 중 사건" value={stats?.confirmed} color="brown" />
              <StatCard label="이번 주 완료" value={stats?.rejected} color="green" />
            </div>

            {/* ── Urgent requests (shown when there are pending items) ── */}
            {recentItems.some((item) => item.status === 'DELIVERED') && (
              <section>
                <div className="flex items-center gap-[7px] mb-2">
                  <div className="w-2 h-2 rounded-[4px] bg-[#a32d2d]" />
                  <span className="text-[13px] font-medium text-[#a32d2d]">
                    빠른 응답이 필요한 의뢰
                  </span>
                </div>
                {recentItems
                  .filter((item) => item.status === 'DELIVERED')
                  .slice(0, 1)
                  .map((item) => (
                    <div
                      key={item.deliveryId}
                      className="bg-white border border-[#f09595] rounded-[14px] p-[14px] space-y-2"
                    >
                      <div className="flex flex-wrap gap-[7px]">
                        <span className="bg-[#e8f0fc] text-[#0c447c] text-[11px] font-medium px-[10px] py-[3px] rounded-full">
                          {DOMAIN_LABELS[item.legalField] ?? item.legalField}
                        </span>
                        <span className="bg-[#e8f0fc] text-[#0c447c] text-[11px] font-medium px-[10px] py-[3px] rounded-full">
                          신규
                        </span>
                      </div>
                      <p className="text-[12px] text-[#6b7280] leading-[19.2px] line-clamp-3">
                        {item.briefTitle}
                      </p>
                      <div className="border-t border-[#e9ecef] pt-[10px] flex items-center justify-between">
                        <div className="flex items-center gap-1">
                          <Clock size={12} className="text-[#a32d2d]" />
                          <span className="text-[11px] font-medium text-[#a32d2d]">24시간 남음</span>
                        </div>
                        <div className="flex items-center gap-[6px]">
                          <button
                            type="button"
                            onClick={() => setConfirmModalOpen(true)}
                            disabled={updateStatus.isPending}
                            className="relative z-10 bg-[#1a6de0] text-white text-[11px] font-medium px-[12px] py-[5px] rounded-full disabled:opacity-60"
                          >
                            수락
                          </button>
                          <button
                            type="button"
                            onClick={() => setRejectModalOpen(true)}
                            disabled={updateStatus.isPending}
                            className="relative z-10 bg-white border border-[#fcebeb] text-[#a32d2d] text-[11px] font-medium px-[11px] py-[6px] rounded-full disabled:opacity-60"
                          >
                            거절
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
              </section>
            )}

            {/* ── Recent requests ─────────────────────────────────────── */}
            <section>
              <div className="flex items-center justify-between pt-1">
                <h2 className="text-[14px] font-medium text-[#1a1a1a]">최근 의뢰</h2>
                <Link
                  to="/lawyer/inbox"
                  className="text-[12px] text-[#1a6de0]"
                >
                  전체 보기 →
                </Link>
              </div>

              {recentItems.length === 0 ? (
                <div className="py-8 text-center">
                  <p className="text-sm text-[#adb5bd]">수신된 의뢰서가 없습니다</p>
                </div>
              ) : (
                <div className="divide-y divide-[#e9ecef]">
                  {recentItems.map((item) => (
                    <RecentItem key={item.deliveryId} item={item} />
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>

      {/* Accept confirm modal */}
      <Modal
        isOpen={confirmModalOpen}
        onClose={() => setConfirmModalOpen(false)}
        title="의뢰 수락"
      >
        <p className="text-sm text-gray-700 mb-5">
          이 의뢰를 수락하시겠습니까?
        </p>
        <div className="flex gap-2">
          <Button
            variant="primary"
            fullWidth
            isLoading={updateStatus.isPending}
            onClick={handleAccept}
          >
            수락
          </Button>
          <Button
            variant="secondary"
            fullWidth
            onClick={() => setConfirmModalOpen(false)}
          >
            취소
          </Button>
        </div>
      </Modal>

      {/* Reject confirm modal */}
      <Modal
        isOpen={rejectModalOpen}
        onClose={() => setRejectModalOpen(false)}
        title="의뢰 거절"
      >
        <div className="mb-5 space-y-3">
          <p className="text-sm text-gray-700">이 의뢰를 거절하시겠습니까?</p>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-[#1E293B]">
              거절 사유 <span className="text-gray-400 font-normal">(선택)</span>
            </label>
            <textarea
              rows={3}
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="거절 사유를 입력해주세요"
              className={rejectTextareaClass}
            />
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            variant="danger"
            fullWidth
            isLoading={updateStatus.isPending}
            onClick={handleReject}
          >
            거절
          </Button>
          <Button
            variant="secondary"
            fullWidth
            onClick={() => setRejectModalOpen(false)}
          >
            취소
          </Button>
        </div>
      </Modal>
    </div>
  );
}
