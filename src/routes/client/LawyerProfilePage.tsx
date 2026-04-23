import { useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { User, MapPin, Award, Briefcase, CheckCircle2 } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useLawyerDetail } from '@/hooks/useLawyer';
import { useDeliverBrief, useDeliveries } from '@/hooks/useBrief';
import { Button, Card, Badge, Spinner, Modal } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';

// ─── page ────────────────────────────────────────────────────────────────────

export function LawyerProfilePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id = '' } = useParams<{ id: string }>();

  // BriefDetailPage 의 "프로필 보기" 에서 navigate state 로 briefId 를 넘겨준 경우에만
  // 전달 플로우 활성화. 일반 변호사 탐색 진입 시에는 전달 CTA 를 숨긴다.
  const briefId = (location.state as { briefId?: string } | null)?.briefId;

  const { data: lawyer, isLoading } = useLawyerDetail(id);
  const deliverBrief = useDeliverBrief(briefId ?? '');

  // 이미 이 의뢰서가 이 변호사에게 전달됐는지 체크
  const { data: deliveries } = useDeliveries(briefId ?? '');
  const alreadyDelivered = (deliveries ?? []).some((d) => d.lawyerId === id);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [successOpen, setSuccessOpen] = useState(false);
  const [alreadyOpen, setAlreadyOpen] = useState(false);

  async function handleDeliver() {
    if (!briefId) return;
    await deliverBrief.mutateAsync(id);
    setConfirmOpen(false);
    setSuccessOpen(true);
  }

  return (
    <div className="flex flex-col flex-1">
      <Header
        title="변호사 프로필"
        showBack
        onBack={() => navigate(-1)}
      />

      <main className="flex-1 px-4 py-4 pb-28 space-y-3">
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center h-64">
            <Spinner size="lg" />
          </div>
        )}

        {/* Not found */}
        {!isLoading && !lawyer && (
          <div className="flex flex-col items-center justify-center gap-4 pt-20 text-center">
            <div className="w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center">
              <User size={36} className="text-gray-300" aria-hidden="true" />
            </div>
            <p className="text-base font-medium text-gray-500">
              변호사 정보를 찾을 수 없습니다
            </p>
          </div>
        )}

        {lawyer && (
          <>
            {/* ── Profile card ── */}
            <Card padding="md">
              <div className="flex flex-col items-center text-center gap-3">
                {/* Avatar */}
                <div
                  className={cn(
                    'w-20 h-20 rounded-full bg-gray-100',
                    'flex items-center justify-center overflow-hidden',
                  )}
                >
                  {lawyer.profileImageUrl ? (
                    <img
                      src={lawyer.profileImageUrl}
                      alt={lawyer.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <User size={36} className="text-gray-400" aria-hidden="true" />
                  )}
                </div>

                {/* Name + experience badge */}
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold text-[#16181d]">{lawyer.name}</h2>
                  <span className="bg-[#f0f7ff] text-[#0680f9] text-xs font-medium px-2.5 py-0.5 rounded-full">
                    경력 {lawyer.experienceYears}년
                  </span>
                </div>

                {/* Specialization — L1 대분류 배열을 Badge 로 나열 */}
                {lawyer.domains && lawyer.domains.length > 0 && (
                  <div className="flex flex-wrap justify-center gap-1.5">
                    {lawyer.domains.map((d: string) => (
                      <Badge key={d} variant="primary" size="sm">
                        {DOMAIN_LABELS[d] ?? d}
                      </Badge>
                    ))}
                  </div>
                )}

                {/* Experience + Region */}
                <div className="flex items-center gap-3 text-sm text-gray-500">
                  <span>{lawyer.experienceYears}년 경력</span>
                  {lawyer.region && (
                    <>
                      <span className="text-gray-300">|</span>
                      <span className="flex items-center gap-1">
                        <MapPin size={13} aria-hidden="true" />
                        {lawyer.region}
                      </span>
                    </>
                  )}
                </div>

                {/* Case count */}
                {lawyer.caseCount > 0 && (
                  <div className="flex items-center gap-1.5 text-sm text-gray-500">
                    <Briefcase size={13} aria-hidden="true" />
                    <span>처리 사건 {lawyer.caseCount}건</span>
                  </div>
                )}
              </div>
            </Card>

            {/* ── Bio card ── */}
            {lawyer.bio && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">소개</h3>
                <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {lawyer.bio}
                </p>
              </Card>
            )}

            {/* ── Tags card ── */}
            {lawyer.tags && lawyer.tags.length > 0 && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">태그</h3>
                <div className="flex flex-wrap gap-1.5">
                  {lawyer.tags.map((tag) => (
                    <span
                      key={tag}
                      className="inline-flex items-center px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-medium"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </Card>
            )}

            {/* ── Certifications card ── */}
            {lawyer.certifications && lawyer.certifications.length > 0 && (
              <Card padding="md">
                <h3 className="text-sm font-semibold text-gray-900 mb-2">자격/인증</h3>
                <ul className="space-y-1.5">
                  {lawyer.certifications.map((cert) => (
                    <li key={cert} className="flex items-center gap-2 text-sm text-gray-700">
                      <Award size={14} className="text-gray-400 flex-shrink-0" aria-hidden="true" />
                      {cert}
                    </li>
                  ))}
                </ul>
              </Card>
            )}
          </>
        )}
      </main>

      {/* ── Bottom fixed CTA — briefId 를 route state 로 받았을 때만 ── */}
      {lawyer && briefId && (
        <div
          className={cn(
            'sticky bottom-0 z-30',
            'bg-white border-t border-gray-200 px-4 py-3 safe-area-bottom',
          )}
        >
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => {
              if (alreadyDelivered) {
                setAlreadyOpen(true);
                return;
              }
              setConfirmOpen(true);
            }}
          >
            의뢰서 전달하기
          </Button>
        </div>
      )}

      {/* ── 전달 확인 모달 ── */}
      <Modal
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        title="의뢰서 전달"
      >
        <p className="text-sm text-gray-700 mb-5">
          <span className="font-semibold text-gray-900">{lawyer?.name}</span> 변호사에게
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
            onClick={() => setConfirmOpen(false)}
          >
            취소
          </Button>
        </div>
      </Modal>

      {/* ── 이미 전달된 변호사 모달 ── */}
      <Modal
        isOpen={alreadyOpen}
        onClose={() => setAlreadyOpen(false)}
        title="전달 불가"
      >
        <p className="text-sm text-gray-700 mb-5">
          <span className="font-semibold text-gray-900">{lawyer?.name}</span> 변호사에게는
          이미 의뢰서를 전달했습니다.
        </p>
        <Button
          variant="primary"
          fullWidth
          onClick={() => setAlreadyOpen(false)}
        >
          확인
        </Button>
      </Modal>

      {/* ── 전달 완료 모달 ── */}
      <Modal
        isOpen={successOpen}
        onClose={() => {
          setSuccessOpen(false);
          if (briefId) navigate(`/briefs/${briefId}/delivery`);
        }}
        title="전달 완료"
      >
        <div className="flex flex-col items-center gap-3 py-2">
          <CheckCircle2 size={48} className="text-brand" aria-hidden="true" />
          <p className="text-base font-semibold text-gray-900">
            의뢰서가 전달되었습니다
          </p>
        </div>
        <div className="mt-5">
          <Button
            variant="primary"
            fullWidth
            onClick={() => {
              setSuccessOpen(false);
              if (briefId) navigate(`/briefs/${briefId}/delivery`);
            }}
          >
            확인
          </Button>
        </div>
      </Modal>
    </div>
  );
}
