import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/dateUtils';
import { useInboxDetail, useUpdateInboxStatus } from '@/hooks/useInbox';
import { Button, Badge, Card, Spinner, Modal } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';

// ─── helpers ────────────────────────────────────────────────────────────────

const textareaClass = cn(
  'w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-sm text-[#1E293B]',
  'placeholder:text-[#64748B] resize-none',
  'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
  'transition-colors duration-150',
);

// ─── page ────────────────────────────────────────────────────────────────────

export function InboxDetailPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: brief, isLoading } = useInboxDetail(id);
  const updateStatus = useUpdateInboxStatus(id);

  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const briefStatus = brief?.status as string | undefined;
  const isPending = briefStatus === 'DELIVERED';

  async function handleAccept() {
    await updateStatus.mutateAsync({ status: 'CONFIRMED' });
    setConfirmModalOpen(false);
    setSuccessMessage('의뢰를 수락했습니다.');
    setTimeout(() => navigate('/lawyer/inbox'), 1500);
  }

  async function handleReject() {
    await updateStatus.mutateAsync({ status: 'REJECTED', rejectionReason: rejectReason || undefined });
    setRejectModalOpen(false);
    setSuccessMessage('의뢰를 거절했습니다.');
    setTimeout(() => navigate('/lawyer/inbox'), 1500);
  }

  // ── loading ──────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="의뢰서 상세" showBack onBack={() => navigate('/lawyer/inbox')} />
        <div className="flex items-center justify-center flex-1">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (!brief) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="의뢰서 상세" showBack onBack={() => navigate('/lawyer/inbox')} />
        <div className="flex items-center justify-center flex-1">
          <p className="text-sm text-gray-500">의뢰서를 찾을 수 없습니다.</p>
        </div>
      </div>
    );
  }

  // ── render ───────────────────────────────────────────────────────────────
  return (
    <div className="flex flex-col flex-1">
      <Header title="의뢰서 상세" showBack onBack={() => navigate('/lawyer/inbox')} />

      <main className="flex-1 px-4 py-4 pb-28">
        {/* Success message */}
        {successMessage && (
          <div className="mb-4 px-4 py-3 bg-green-50 border border-green-200 rounded-xl">
            <p className="text-sm text-green-700 font-medium">{successMessage}</p>
          </div>
        )}

        {/* 24-hour warning bar */}
        {isPending && (
          <div className="mb-4 bg-red-50 rounded-[10px] px-3 py-2.5 flex items-center gap-2">
            <span className="text-xs text-red-700">
              24시간 이내 응답 없으면 자동 거절됩니다. <strong>12시간 남음</strong>
            </span>
          </div>
        )}

        <Card padding="md" className="space-y-5">
          {/* Title + status */}
          <div className="flex items-start justify-between gap-2">
            <h2 className="text-base font-semibold text-gray-900 leading-snug flex-1">
              {brief.title}
            </h2>
            <Badge
              variant={
                briefStatus === 'CONFIRMED'
                  ? 'success'
                  : briefStatus === 'REJECTED'
                  ? 'danger'
                  : 'primary'
              }
              size="sm"
            >
              {briefStatus === 'DELIVERED'
                ? '대기 중'
                : briefStatus === 'CONFIRMED'
                ? '수락'
                : briefStatus === 'REJECTED'
                ? '거절'
                : brief.status}
            </Badge>
          </div>

          {/* Legal field + date */}
          <div className="flex items-center gap-2">
            <Badge variant="default" size="sm">
              {DOMAIN_LABELS[brief.legalField] ?? brief.legalField}
            </Badge>
            <span className="text-xs text-gray-400">{formatDate(brief.sentAt)}</span>
          </div>

          {/* Content */}
          {brief.content && (
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5">
                내용
              </p>
              <p className="text-sm text-gray-800 leading-relaxed whitespace-pre-wrap">
                {brief.content}
              </p>
            </div>
          )}

          {/* Key issues */}
          {brief.keyIssues && brief.keyIssues.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5">
                주요 쟁점
              </p>
              <ul className="space-y-1">
                {brief.keyIssues.map((issue, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm text-gray-800">
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-brand flex-shrink-0" />
                    {issue.title}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Keywords */}
          {brief.keywords && brief.keywords.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5">
                키워드
              </p>
              <div className="flex flex-wrap gap-1.5">
                {brief.keywords.map((kw, i) => (
                  <span
                    key={i}
                    className="inline-flex items-center px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-medium"
                  >
                    {kw}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Client info */}
          {brief.clientName && (
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5">
                의뢰인
              </p>
              <p className="text-sm text-gray-800">{brief.clientName}</p>
            </div>
          )}
        </Card>
      </main>

      {/* Bottom action bar */}
      {isPending && !successMessage && (
        <div className="sticky bottom-0 bg-white px-5 py-4 safe-area-bottom space-y-2.5">
          <Button
            variant="primary"
            fullWidth
            size="lg"
            className="rounded-pill"
            onClick={() => setConfirmModalOpen(true)}
          >
            수락하기
          </Button>
          <Button
            variant="secondary"
            fullWidth
            size="lg"
            className="rounded-pill border-red-200 text-red-600 hover:bg-red-50"
            onClick={() => setRejectModalOpen(true)}
          >
            거절하기
          </Button>
        </div>
      )}

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
              className={textareaClass}
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
