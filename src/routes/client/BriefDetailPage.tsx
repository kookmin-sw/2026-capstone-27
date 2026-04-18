import { useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { Edit2, Check, X, ChevronRight, User } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { cn } from '@/lib/cn';
import {
  useBriefDetail,
  useUpdateBrief,
  useConfirmBrief,
  useLawyerRecommendations,
  useDeliverBrief,
} from '@/hooks/useBrief';
import { Button, Card, Badge, Spinner, Modal, Input } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS, BRIEF_STATUS_LABELS, BRIEF_STATUS_BADGE } from '@/lib/constants';
import type { BriefUpdateRequest } from '@/types/brief';

// ─── helpers ────────────────────────────────────────────────────────────────

interface EditFormValues {
  title: string;
  content: string;
  keyIssues: string; // comma-separated
  keywords: string; // comma-separated
  strategy: string;
}

// ─── page ────────────────────────────────────────────────────────────────────

export function BriefDetailPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: brief, isLoading } = useBriefDetail(id);
  const updateBrief = useUpdateBrief(id);
  const confirmBrief = useConfirmBrief(id);
  const deliverBrief = useDeliverBrief(id);

  const isEditable = brief?.status === 'DRAFT';
  const isConfirmed = brief?.status === 'CONFIRMED' || brief?.status === 'DELIVERED';

  // Lawyer recommendations — only enabled after confirmed
  const { data: recommendations, isLoading: recLoading } = useLawyerRecommendations(
    id,
    isConfirmed,
  );

  // Edit mode state
  const [editMode, setEditMode] = useState(false);

  // Delivery confirmation modal
  const [deliverTarget, setDeliverTarget] = useState<{ lawyerId: string; name: string } | null>(
    null,
  );
  const [deliverSuccess, setDeliverSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<EditFormValues>();

  function enterEditMode() {
    if (!brief) return;
    reset({
      title: brief.title,
      content: brief.content,
      keyIssues: brief.keyIssues.map(ki => ki.title).join(', '),
      keywords: brief.keywords.join(', '),
      strategy: brief.strategy,
    });
    setEditMode(true);
  }

  function cancelEdit() {
    setEditMode(false);
  }

  async function onSave(values: EditFormValues) {
    const updates: BriefUpdateRequest = {
      title: values.title,
      content: values.content,
      keyIssues: values.keyIssues
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
        .map((title) => ({ title, description: '' })),
      keywords: values.keywords
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
      strategy: values.strategy,
    };
    await updateBrief.mutateAsync(updates);
    setEditMode(false);
  }

  async function handleConfirm() {
    await confirmBrief.mutateAsync();
  }

  async function handleDeliver() {
    if (!deliverTarget) return;
    await deliverBrief.mutateAsync(deliverTarget.lawyerId);
    setDeliverTarget(null);
    setDeliverSuccess(true);
  }

  // ── header right action ──────────────────────────────────────────────────
  const editButton = isEditable ? (
    <button
      type="button"
      onClick={editMode ? cancelEdit : enterEditMode}
      aria-label={editMode ? '수정 취소' : '수정'}
      className={cn(
        'flex items-center gap-1 text-sm font-medium',
        editMode ? 'text-gray-500' : 'text-brand',
        'hover:opacity-80 transition-opacity',
        '-mr-1 px-1 py-1',
      )}
    >
      {editMode ? <X size={16} /> : <Edit2 size={16} />}
      <span>{editMode ? '취소' : '수정'}</span>
    </button>
  ) : undefined;

  // ── loading ──────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="의뢰서" showBack onBack={() => navigate('/briefs')} />
        <div className="flex items-center justify-center flex-1">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (!brief) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="의뢰서" showBack onBack={() => navigate('/briefs')} />
        <div className="flex items-center justify-center flex-1">
          <p className="text-sm text-gray-500">의뢰서를 찾을 수 없습니다.</p>
        </div>
      </div>
    );
  }

  // ── render ───────────────────────────────────────────────────────────────
  return (
    <div className="flex flex-col flex-1">
      <Header
        title={brief.title}
        showBack
        onBack={() => navigate('/briefs')}
        rightAction={editButton}
      />

      <main className="flex-1 px-4 py-4 space-y-4 pb-10">
        {/* ── Brief content card ──────────────────────────────────────── */}
        <Card padding="md">
          {editMode ? (
            /* ── Edit form ─── */
            <form onSubmit={handleSubmit(onSave)} className="space-y-4">
              <Input
                label="제목"
                placeholder="의뢰서 제목"
                {...register('title', { required: true })}
              />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[#1E293B]">내용</label>
                <textarea
                  rows={5}
                  placeholder="사건의 자세한 내용을 입력하세요"
                  className={cn(
                    'w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-sm text-[#1E293B]',
                    'placeholder:text-[#64748B] resize-none',
                    'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
                    'transition-colors duration-150',
                  )}
                  {...register('content')}
                />
              </div>
              <Input
                label="주요 쟁점 (쉼표로 구분)"
                placeholder="예: 손해배상 청구, 과실 비율 다툼"
                {...register('keyIssues')}
              />
              <Input
                label="키워드 (쉼표로 구분)"
                placeholder="예: 교통사고, 손해배상"
                {...register('keywords')}
              />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[#1E293B]">전략</label>
                <textarea
                  rows={3}
                  placeholder="법적 전략을 입력하세요"
                  className={cn(
                    'w-full rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-sm text-[#1E293B]',
                    'placeholder:text-[#64748B] resize-none',
                    'outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand',
                    'transition-colors duration-150',
                  )}
                  {...register('strategy')}
                />
              </div>
              <div className="flex gap-2 pt-1">
                <Button
                  type="submit"
                  variant="primary"
                  fullWidth
                  isLoading={isSubmitting}
                  leftIcon={<Check size={15} />}
                >
                  저장
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  fullWidth
                  onClick={cancelEdit}
                >
                  취소
                </Button>
              </div>
            </form>
          ) : (
            /* ── View mode ─── */
            <div className="space-y-4">
              {/* Title + status + domain */}
              <div className="flex items-start justify-between gap-2">
                <h2 className="text-base font-semibold text-gray-900 leading-snug flex-1">
                  {brief.title}
                </h2>
                <Badge variant={BRIEF_STATUS_BADGE[brief.status]} size="sm">
                  {BRIEF_STATUS_LABELS[brief.status] ?? brief.status}
                </Badge>
              </div>

              {/* Legal field */}
              <div>
                <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-gray-100 text-gray-700 text-xs font-medium">
                  {DOMAIN_LABELS[brief.legalField] ?? brief.legalField}
                </span>
              </div>

              {/* Content */}
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
                  내용
                </p>
                <p className="text-sm text-gray-800 leading-relaxed whitespace-pre-wrap">
                  {brief.content}
                </p>
              </div>

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

              {/* Strategy */}
              {brief.strategy && (
                <div>
                  <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
                    전략
                  </p>
                  <p className="text-sm text-gray-800 leading-relaxed whitespace-pre-wrap">
                    {brief.strategy}
                  </p>
                </div>
              )}

              {/* CTA buttons for DRAFT / AWAITING_CONFIRM */}
              {isEditable && (
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="primary"
                    fullWidth
                    isLoading={confirmBrief.isPending}
                    onClick={handleConfirm}
                    leftIcon={<Check size={15} />}
                  >
                    의뢰서 확정
                  </Button>
                  <Button
                    variant="secondary"
                    fullWidth
                    onClick={enterEditMode}
                    leftIcon={<Edit2 size={15} />}
                  >
                    수정
                  </Button>
                </div>
              )}
            </div>
          )}
        </Card>

        {/* ── Lawyer recommendations (CONFIRMED / DELIVERED) ───────────── */}
        {isConfirmed && (
          <section>
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-sm font-semibold text-gray-700">추천 변호사</h2>
              {deliverSuccess && (
                <Link
                  to={`/briefs/${id}/delivery`}
                  className="flex items-center gap-0.5 text-xs text-brand hover:text-blue-700 font-medium"
                >
                  전달 현황 보기
                  <ChevronRight size={14} aria-hidden="true" />
                </Link>
              )}
            </div>

            {recLoading && (
              <div className="flex items-center justify-center h-32">
                <Spinner size="md" text="변호사를 찾고 있습니다..." />
              </div>
            )}

            {!recLoading && (!recommendations || recommendations.length === 0) && (
              <Card padding="md" className="text-center py-6">
                <p className="text-sm text-gray-400">추천 변호사가 없습니다</p>
              </Card>
            )}

            {!recLoading && recommendations && recommendations.length > 0 && (
              <div className="flex flex-col gap-3">
                {recommendations.map((lawyer) => (
                  <Card key={lawyer.lawyerId} padding="md">
                    <div className="flex items-start gap-3">
                      {/* Avatar */}
                      <div className="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center flex-shrink-0 overflow-hidden">
                        {lawyer.profileImageUrl ? (
                          <img
                            src={lawyer.profileImageUrl}
                            alt={lawyer.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <User size={20} className="text-blue-300" aria-hidden="true" />
                        )}
                      </div>

                      <div className="flex-1 min-w-0">
                        {/* Name + experience */}
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-semibold text-gray-900">{lawyer.name}</p>
                          <span className="text-xs text-gray-500">
                            경력 {lawyer.experienceYears}년
                          </span>
                        </div>

                        {/* Specializations */}
                        {lawyer.specializations && (
                          <p className="text-xs text-gray-600 mb-2 line-clamp-1">
                            {lawyer.specializations}
                          </p>
                        )}

                        {/* Matched keywords */}
                        {lawyer.matchedKeywords && lawyer.matchedKeywords.length > 0 && (
                          <div className="flex flex-wrap gap-1 mb-3">
                            {lawyer.matchedKeywords.map((kw, i) => (
                              <span
                                key={i}
                                className="inline-flex items-center px-2 py-0.5 rounded-full bg-green-50 text-green-700 text-xs font-medium"
                              >
                                {kw}
                              </span>
                            ))}
                          </div>
                        )}

                        {/* Actions */}
                        <div className="flex gap-2">
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => navigate(`/lawyers/${lawyer.lawyerId}`)}
                          >
                            프로필 보기
                          </Button>
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() =>
                              setDeliverTarget({
                                lawyerId: lawyer.lawyerId,
                                name: lawyer.name,
                              })
                            }
                          >
                            전달
                          </Button>
                        </div>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            )}

            {/* After delivery: link to delivery status */}
            {deliverSuccess && (
              <div className="mt-3 text-center">
                <Link
                  to={`/briefs/${id}/delivery`}
                  className="inline-flex items-center gap-1 text-sm text-brand hover:text-blue-700 font-medium transition-colors"
                >
                  전달 현황 보기
                  <ChevronRight size={15} aria-hidden="true" />
                </Link>
              </div>
            )}
          </section>
        )}
      </main>

      {/* ── Deliver confirmation modal ───────────────────────────────────── */}
      <Modal
        isOpen={!!deliverTarget}
        onClose={() => setDeliverTarget(null)}
        title="의뢰서 전달"
      >
        <p className="text-sm text-gray-700 mb-5">
          <span className="font-semibold text-gray-900">{deliverTarget?.name}</span> 변호사에게
          의뢰서를 전달하시겠습니까?
        </p>
        <div className="flex gap-2">
          <Button
            variant="primary"
            fullWidth
            isLoading={deliverBrief.isPending}
            onClick={handleDeliver}
          >
            전달하기
          </Button>
          <Button
            variant="secondary"
            fullWidth
            onClick={() => setDeliverTarget(null)}
          >
            취소
          </Button>
        </div>
      </Modal>
    </div>
  );
}
