import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { RefreshCw } from 'lucide-react';
import { cn } from '@/lib/cn';
import { usePolling } from '@/hooks/usePolling';
import { consultationApi } from '@/lib/consultationApi';
import { Button, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { ConsultationResponse } from '@/types/consultation';

// ─── page ────────────────────────────────────────────────────────────────────

export function AnalyzingPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [timedOut, setTimedOut] = useState(false);
  const [elapsedSecs, setElapsedSecs] = useState(0);

  // ── elapsed timer ───────────────────────────────────────────────────────
  useEffect(() => {
    const start = Date.now();
    const timer = setInterval(() => {
      setElapsedSecs(Math.floor((Date.now() - start) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // ── navigate on complete ────────────────────────────────────────────────
  const handleComplete = useCallback(
    (data: ConsultationResponse) => {
      // Sprint 3 placeholder — navigate to briefs when status leaves ANALYZING
      if (data.status !== 'ANALYZING') {
        navigate('/briefs', { replace: true });
      }
    },
    [navigate],
  );

  const handleTimeout = useCallback(() => {
    setTimedOut(true);
  }, []);

  // ── polling ─────────────────────────────────────────────────────────────
  const { isPolling } = usePolling<ConsultationResponse>({
    fn: () => consultationApi.getById(id).then((r) => r.data.data),
    interval: 5000,
    maxDuration: 60000,
    enabled: !!id && !timedOut,
    shouldStop: (data) => data.status !== 'ANALYZING',
    onComplete: handleComplete,
    onTimeout: handleTimeout,
  });

  // ── retry — remounts the page (clears timedOut) ─────────────────────────
  function handleRetry() {
    setTimedOut(false);
    setElapsedSecs(0);
  }

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header
        title="의뢰서 생성 중"
        showBack
        onBack={() => navigate('/consultations')}
      />

      <main className="flex-1 flex flex-col items-center justify-center px-6 gap-6 text-center">
        {timedOut ? (
          /* ── timeout state ──────────────────────────────────────────── */
          <>
            <div className="w-20 h-20 rounded-full bg-red-50 flex items-center justify-center">
              <RefreshCw size={36} className="text-red-400" />
            </div>
            <div className="space-y-1.5">
              <p className="text-base font-semibold text-gray-800">
                시간이 초과되었습니다
              </p>
              <p className="text-sm text-gray-500">
                새로고침해주세요.
              </p>
            </div>
            <Button
              variant="primary"
              size="md"
              leftIcon={<RefreshCw size={16} />}
              onClick={handleRetry}
            >
              다시 시도
            </Button>
          </>
        ) : (
          /* ── polling (loading) state ────────────────────────────────── */
          <>
            {/* Large animated spinner */}
            <div
              className={cn(
                'w-24 h-24 rounded-full bg-blue-50',
                'flex items-center justify-center',
              )}
            >
              <Spinner size="lg" className="text-brand" />
            </div>

            {/* Main copy */}
            <div className="space-y-2">
              <p className="text-base font-semibold text-gray-900">
                AI가 의뢰서를 작성하고 있습니다...
              </p>
              <p className="text-sm text-gray-500">
                잠시만 기다려주세요
              </p>
            </div>

            {/* Progress hint */}
            <p className="text-xs text-gray-400 bg-gray-100 px-4 py-2 rounded-pill">
              보통 30초~1분 정도 소요됩니다
            </p>

            {/* Elapsed time */}
            <div className="mt-2">
              <p className="text-xs text-gray-400">
                경과 시간:{' '}
                <span className="font-medium tabular-nums">
                  {elapsedSecs}초
                </span>
              </p>
              {!isPolling && (
                <p className="text-xs text-gray-300 mt-0.5">
                  마지막 확인 중...
                </p>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
