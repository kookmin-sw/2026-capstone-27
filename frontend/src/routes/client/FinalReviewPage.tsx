import { useNavigate, useParams, Link } from 'react-router-dom';
import { FileText, Shield, Scale, Pencil, TriangleAlert } from 'lucide-react';
import { useBriefDetail } from '@/hooks/useBrief';
import { Button, Card, Badge, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import { DOMAIN_LABELS } from '@/lib/constants';

// ─── helpers ────────────────────────────────────────────────────────────────

function SectionHeader({
  title,
  editHref,
}: {
  title: string;
  editHref: string;
}) {
  return (
    <div className="flex items-center justify-between mb-3">
      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">{title}</p>
      <Link
        to={editHref}
        className="inline-flex items-center gap-1 text-xs font-medium text-brand hover:text-blue-700 transition-colors"
      >
        <Pencil size={11} aria-hidden="true" />
        수정
      </Link>
    </div>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function FinalReviewPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: brief, isLoading } = useBriefDetail(id);

  // ── loading ──────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="최종 검토" showBack onBack={() => navigate(-1)} />
        <div className="flex items-center justify-center flex-1">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (!brief) {
    return (
      <div className="flex flex-col flex-1">
        <Header title="최종 검토" showBack onBack={() => navigate(-1)} />
        <div className="flex items-center justify-center flex-1">
          <p className="text-sm text-gray-500">의뢰서를 찾을 수 없습니다.</p>
        </div>
      </div>
    );
  }

  const contentExcerpt =
    brief.content.length > 120 ? brief.content.slice(0, 120) + '…' : brief.content;

  // Privacy setting labels
  const privacyLabels: Record<string, string> = {
    PUBLIC: '공개',
    PRIVATE: '비공개',
    PARTIAL: '일부 공개',
  };

  function handleSendRequest() {
    navigate(`/briefs/${id}/confirm`);
  }

  return (
    <div className="flex flex-col flex-1">
      <Header title="최종 검토" showBack onBack={() => navigate(-1)} />

      <main className="flex-1 px-6 py-6 space-y-5 pb-36">
        {/* Intro */}
        <div>
          <h2 className="text-xl font-bold text-[#161a1d] tracking-tight leading-8">
            마지막으로
            <br />
            내용을 확인해주세요.
          </h2>
          <p className="text-sm text-[#31383f] mt-2">
            작성하신 의뢰서가 변호사님께 전달됩니다.
          </p>
        </div>

        {/* Case Summary Card */}
        <Card padding="md">
          <SectionHeader title="사건 요약" editHref={`/briefs/${id}`} />

          <div className="space-y-3">
            {/* Title */}
            <div>
              <p className="text-xs text-gray-400 mb-0.5">제목</p>
              <div className="flex items-start gap-2">
                <FileText size={15} className="text-brand flex-shrink-0 mt-0.5" aria-hidden="true" />
                <p className="text-sm font-semibold text-gray-900 leading-snug">{brief.title}</p>
              </div>
            </div>

            {/* Legal field */}
            <div>
              <p className="text-xs text-gray-400 mb-1">법률 분야</p>
              <Badge variant="primary" size="sm">
                {DOMAIN_LABELS[brief.legalField] ?? brief.legalField}
              </Badge>
            </div>

            {/* Content excerpt */}
            <div>
              <p className="text-xs text-gray-400 mb-0.5">내용 요약</p>
              <p className="text-sm text-gray-700 leading-relaxed">{contentExcerpt}</p>
            </div>

            {/* Key issues */}
            {brief.keyIssues && brief.keyIssues.length > 0 && (
              <div>
                <p className="text-xs text-gray-400 mb-1.5">주요 쟁점</p>
                <ul className="space-y-1">
                  {brief.keyIssues.map((issue, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
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
                <p className="text-xs text-gray-400 mb-1.5">키워드</p>
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
          </div>
        </Card>

        {/* Privacy Settings Summary */}
        <Card padding="md">
          <SectionHeader title="개인정보 설정" editHref={`/briefs/${id}/privacy`} />

          <div className="space-y-2.5">
            <div className="flex items-center gap-3">
              <Shield size={15} className="text-brand flex-shrink-0" aria-hidden="true" />
              <div className="flex-1 flex items-center justify-between">
                <span className="text-sm text-gray-700">공개 설정</span>
                <span className="text-sm font-medium text-gray-900">
                  {privacyLabels[brief.privacySetting] ?? brief.privacySetting}
                </span>
              </div>
            </div>
            <div className="rounded-lg bg-blue-50 px-3 py-2.5">
              <p className="text-xs text-blue-700 leading-relaxed">
                사건 상세 공유 · AI 모델 개선 동의 · 푸시 알림 허용
              </p>
            </div>
          </div>
        </Card>

        {/* Lawyer Requirements */}
        <Card padding="md">
          <SectionHeader title="변호사 요건" editHref={`/briefs/${id}`} />

          <div className="flex items-center gap-3">
            <Scale size={15} className="text-gray-400 flex-shrink-0" aria-hidden="true" />
            <p className="text-sm text-gray-500">
              {brief.strategy
                ? brief.strategy
                : '특별한 요건이 설정되지 않았습니다.'}
            </p>
          </div>
        </Card>

        {/* Warning box */}
        <div className="bg-red-500/5 border border-red-500/20 rounded-card p-4 flex gap-3">
          <div className="shrink-0 w-9 h-9 rounded-[18px] bg-red-500/10 flex items-center justify-center mt-1">
            <TriangleAlert size={20} className="text-red-500" />
          </div>
          <div>
            <p className="text-sm font-bold text-red-500 mb-1">제출 전 확인 필수</p>
            <p className="text-[10px] text-red-500/80 leading-4.5">
              의뢰서 제출 버튼을 누른 후에는{' '}
              <span className="underline">내용을 수정할 수 없습니다.</span>
              <br />
              모든 항목이 정확한지 다시 한번 확인해 주세요.
            </p>
          </div>
        </div>
      </main>

      {/* Fixed bottom CTA */}
      <div className="sticky bottom-0 bg-white shadow-[0px_-10px_20px_0px_rgba(0,0,0,0.02)] px-6 py-6 safe-area-bottom">
        <Button
          variant="primary"
          fullWidth
          size="lg"
          onClick={handleSendRequest}
          className="rounded-card h-14 shadow-lg shadow-brand/20"
        >
          변호사 선택
        </Button>
        <p className="text-[10px] text-[#31383f] text-center mt-3">
          제출 시 SHIELD 서비스 이용 약관 및 개인정보 처리방침에 동의하게 됩니다.
        </p>
      </div>
    </div>
  );
}
