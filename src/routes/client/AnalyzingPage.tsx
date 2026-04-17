import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { RefreshCw, Shield, Info } from 'lucide-react';
import { usePolling } from '@/hooks/usePolling';
import { consultationApi } from '@/lib/consultationApi';
import { Button, Spinner } from '@/components/ui';
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
  usePolling<ConsultationResponse>({
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
    <div className="flex flex-col min-h-dvh bg-white">
      <main className="flex-1 flex flex-col items-center justify-center px-6 text-center">
        {timedOut ? (
          /* ── timeout state ──────────────────────────────────────────── */
          <>
            <div className="w-20 h-20 rounded-full bg-red-50 flex items-center justify-center">
              <RefreshCw size={36} className="text-red-400" />
            </div>
            <div className="space-y-1.5 mt-6">
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
              className="mt-6"
            >
              다시 시도
            </Button>
          </>
        ) : (
          /* ── polling (loading) state ────────────────────────────────── */
          <>
            {/* SHIELD branding */}
            <div className="w-14 h-14 rounded-[28px] bg-[#161a1d] flex items-center justify-center">
              <Shield size={32} className="text-white" strokeWidth={1.5} />
            </div>
            <p className="text-[22px] font-bold text-[#161a1d] mt-2">SHIELD</p>
            <p className="text-xs font-medium text-[#31383f] tracking-widest uppercase">
              Legal Intelligence System
            </p>

            {/* Loading animation */}
            <div className="mt-10 mb-6 relative">
              <div className="w-24 h-24 rounded-full border-4 border-brand/10 flex items-center justify-center">
                <Spinner size="lg" className="text-brand" />
              </div>
            </div>

            {/* Main copy */}
            <p className="text-xl font-bold text-[#161a1d] tracking-tight">
              사건을 분석하고 있습니다...
            </p>
            <p className="text-xs text-[#31383f] mt-2 leading-relaxed max-w-68.5">
              입력하신 내용을 바탕으로 최적의 법률 프레임워크를 구성 중입니다.
            </p>

            {/* Duration badge */}
            <div className="mt-6 flex items-center gap-2 bg-[#f3f5f6] border border-[#dde0e4] rounded-pill px-4 py-2 shadow-sm">
              <Info size={16} className="text-[#31383f]" />
              <span className="text-sm font-medium text-[#1d2125]">약 10~30초 소요</span>
            </div>

            {/* Elapsed */}
            <p className="text-xs text-gray-400 mt-4 tabular-nums">
              {elapsedSecs}초 경과
            </p>
          </>
        )}
      </main>

      {/* Bottom security notice */}
      {!timedOut && (
        <div className="pb-8 pt-4 text-center">
          <p className="text-[10px] font-bold text-[#31383f]/60 uppercase tracking-tight">
            Secure Data Processing
          </p>
          <p className="text-[11px] text-[#31383f]/40 mt-1 px-12 leading-relaxed">
            SHIELD는 모든 데이터를 암호화하여 처리하며, 분석 완료 후 안전하게 결과를 전달합니다.
          </p>
        </div>
      )}
    </div>
  );
}
