import { useNavigate, useParams } from 'react-router-dom';
import { Check, Clock, MessageSquare, Calendar } from 'lucide-react';
import { cn } from '@/lib/cn';
import { Button, Card, Badge } from '@/components/ui';
import { Header } from '@/components/layout/Header';

// ─── types ───────────────────────────────────────────────────────────────────

type StepStatus = 'completed' | 'active' | 'pending';

interface TrackingStep {
  label: string;
  status: StepStatus;
  timestamp?: string;
}

interface RequestCard {
  id: string;
  category: string;
  lawyerName: string;
  submittedAt: string;
  status: 'pending' | 'active' | 'complete';
  expectedResponse: string;
}

// ─── mock data ───────────────────────────────────────────────────────────────

const MOCK_STEPS: TrackingStep[] = [
  { label: '요청 전송', status: 'completed', timestamp: '방금 전' },
  { label: '변호사 확인 중', status: 'active' },
  { label: '응답 대기', status: 'pending' },
  { label: '상담 확정', status: 'pending' },
];

const MOCK_REQUESTS: RequestCard[] = [
  {
    id: '1',
    category: '민사',
    lawyerName: '김민준 변호사',
    submittedAt: '2026.04.16 14:32',
    status: 'active',
    expectedResponse: '24시간 이내',
  },
];

// ─── Stepper ─────────────────────────────────────────────────────────────────

function StepIndicator({ status }: { status: StepStatus }) {
  if (status === 'completed') {
    return (
      <div className="w-7 h-7 rounded-full bg-brand flex items-center justify-center flex-shrink-0">
        <Check size={14} className="text-white" strokeWidth={2.5} />
      </div>
    );
  }
  if (status === 'active') {
    return (
      <div className="w-7 h-7 rounded-full border-2 border-brand flex items-center justify-center flex-shrink-0 relative">
        <span className="w-2.5 h-2.5 rounded-full bg-brand animate-pulse" />
      </div>
    );
  }
  return (
    <div className="w-7 h-7 rounded-full border-2 border-gray-300 flex items-center justify-center flex-shrink-0">
      <span className="w-2.5 h-2.5 rounded-full bg-gray-300" />
    </div>
  );
}

function VerticalStepper({ steps }: { steps: TrackingStep[] }) {
  return (
    <div className="flex flex-col gap-0">
      {steps.map((step, i) => {
        const isLast = i === steps.length - 1;
        return (
          <div key={step.label} className="flex gap-3">
            {/* Left: indicator + connector */}
            <div className="flex flex-col items-center">
              <StepIndicator status={step.status} />
              {!isLast && (
                <div
                  className={cn(
                    'w-0.5 flex-1 min-h-[28px]',
                    step.status === 'completed' ? 'bg-brand' : 'bg-gray-200',
                  )}
                />
              )}
            </div>

            {/* Right: label + timestamp */}
            <div
              className={cn(
                'pb-6 pt-0.5',
                isLast && 'pb-0',
              )}
            >
              <p
                className={cn(
                  'text-sm font-semibold leading-none',
                  step.status === 'completed' && 'text-brand',
                  step.status === 'active' && 'text-gray-900',
                  step.status === 'pending' && 'text-gray-400',
                )}
              >
                {step.label}
              </p>
              {step.timestamp && (
                <p className="text-xs text-gray-400 mt-1">{step.timestamp}</p>
              )}
              {step.status === 'active' && !step.timestamp && (
                <p className="text-xs text-brand mt-1">진행 중</p>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ─── Request card ─────────────────────────────────────────────────────────────

const STATUS_BADGE: Record<RequestCard['status'], { variant: 'warning' | 'primary' | 'success'; label: string }> = {
  pending: { variant: 'warning', label: '대기 중' },
  active: { variant: 'primary', label: '확인 중' },
  complete: { variant: 'success', label: '완료' },
};

function ActiveRequestCard({ request }: { request: RequestCard }) {
  const badge = STATUS_BADGE[request.status];

  return (
    <Card padding="md">
      <div className="space-y-3">
        {/* Header row */}
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="text-xs text-gray-400 mb-1">의뢰 분야</p>
            <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-medium">
              {request.category}
            </span>
          </div>
          <Badge variant={badge.variant} size="sm">
            {badge.label}
          </Badge>
        </div>

        <div className="h-px bg-gray-100" />

        {/* Details */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm">
            <MessageSquare size={14} className="text-gray-400 flex-shrink-0" aria-hidden="true" />
            <span className="text-gray-500">변호사</span>
            <span className="font-medium text-gray-900 ml-auto">{request.lawyerName}</span>
          </div>
          <div className="flex items-center gap-2 text-sm">
            <Calendar size={14} className="text-gray-400 flex-shrink-0" aria-hidden="true" />
            <span className="text-gray-500">요청 일시</span>
            <span className="font-medium text-gray-900 ml-auto">{request.submittedAt}</span>
          </div>
          <div className="flex items-center gap-2 text-sm">
            <Clock size={14} className="text-gray-400 flex-shrink-0" aria-hidden="true" />
            <span className="text-gray-500">예상 응답</span>
            <span className="font-medium text-gray-900 ml-auto">{request.expectedResponse}</span>
          </div>
        </div>
      </div>
    </Card>
  );
}

// ─── page ────────────────────────────────────────────────────────────────────

export function RequestTrackingPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header
        title="요청 현황"
        showBack
        onBack={() => navigate(`/briefs/${id}`)}
      />

      <main className="flex-1 px-4 py-6 space-y-5 pb-10">
        {/* Progress stepper section */}
        <Card padding="md">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-4">
            진행 단계
          </p>
          <VerticalStepper steps={MOCK_STEPS} />
        </Card>

        {/* Active requests */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">진행 중인 요청</h2>

          {MOCK_REQUESTS.length === 0 ? (
            <Card padding="md" className="text-center py-8">
              <p className="text-sm text-gray-400">진행 중인 요청이 없습니다.</p>
            </Card>
          ) : (
            <div className="space-y-3">
              {MOCK_REQUESTS.map((req) => (
                <ActiveRequestCard key={req.id} request={req} />
              ))}
            </div>
          )}
        </section>

        {/* Info box */}
        <div className="rounded-xl bg-blue-50 px-4 py-3.5 flex gap-3">
          <Clock size={15} className="text-brand flex-shrink-0 mt-0.5" aria-hidden="true" />
          <p className="text-xs text-blue-700 leading-relaxed">
            변호사가 요청을 확인하면 알림을 통해 안내됩니다. 24시간 내에 응답이 없을 경우 고객센터로 문의해 주세요.
          </p>
        </div>

        {/* Bottom action */}
        <div className="pt-2">
          <Button
            variant="secondary"
            fullWidth
            onClick={() => navigate('/lawyers')}
          >
            다른 변호사 찾기
          </Button>
        </div>
      </main>
    </div>
  );
}
