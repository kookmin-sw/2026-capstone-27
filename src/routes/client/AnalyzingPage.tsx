import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { RefreshCw, Shield, Scale } from 'lucide-react';
import { usePolling } from '@/hooks/usePolling';
import { consultationApi } from '@/lib/consultationApi';
import { DOMAIN_LABELS } from '@/lib/constants';
import { Button, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { ConsultationResponse } from '@/types/consultation';

// ─── page ────────────────────────────────────────────────────────────────────

export function AnalyzingPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [timedOut, setTimedOut] = useState(false);
  const [elapsedSecs, setElapsedSecs] = useState(0);
  const [classificationResult, setClassificationResult] = useState<ConsultationResponse | null>(null);

  // ── elapsed timer ───────────────────────────────────────────────────────
  useEffect(() => {
    if (classificationResult) return; // stop timer when result arrives
    const start = Date.now();
    const timer = setInterval(() => {
      setElapsedSecs(Math.floor((Date.now() - start) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, [classificationResult]);

  // ── navigate on complete → show classification result ───────────────────
  const handleComplete = useCallback(
    (data: ConsultationResponse) => {
      if (data.status !== 'ANALYZING') {
        setClassificationResult(data);
      }
    },
    [],
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

  // ── classification result view ───────────────────────────────────────────
  if (classificationResult) {
    const primaryDomain = classificationResult.primaryField?.[0] ?? '';
    const domainLabel = DOMAIN_LABELS[primaryDomain] ?? primaryDomain;
    const tags = classificationResult.tags ?? [];

    return (
      <div className="flex flex-col flex-1">
        <Header
          title="분류 결과"
          showBack
          onBack={() => navigate(`/consultations/${id}`)}
        />
        <main className="flex-1 flex flex-col px-5 py-6">
          <h2 className="text-xl font-bold text-[#181b20] leading-8">
            사건 분류가 완료되었습니다
          </h2>
          <p className="mt-2 text-sm text-[#555d6d] leading-relaxed">
            입력하신 내용을 바탕으로 AI가 가장 유사한 법률 분야를 선정했습니다. 결과를 확인해 주세요.
          </p>

          {/* Classification result card */}
          <div className="mt-6 bg-[#d8ebfd] rounded-[10px] shadow-lg p-6 flex flex-col items-center">
            <div className="w-18 h-18 rounded-full bg-white/60 flex items-center justify-center mb-4">
              <Scale size={32} className="text-brand" />
            </div>
            <p className="text-sm font-medium text-brand/70">AI가 분석한 주요 분야</p>
            <p className="text-4xl font-bold text-brand mt-1">{domainLabel}</p>
            {tags.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-4 justify-center">
                {tags.map((tag) => (
                  <span
                    key={tag}
                    className="bg-white/80 text-[#31383f] text-xs font-medium px-3 py-1 rounded-full"
                  >
                    #{tag}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Info box */}
          <div className="mt-4 bg-gray-50 border border-gray-200 rounded-[10px] px-4 py-3">
            <p className="text-xs text-[#555d6d] text-center leading-relaxed">
              AI의 분석은 틀릴 수 있습니다. 다른 법률 분야를 선택하시겠습니까?
            </p>
          </div>

          <div className="flex-1" />

          {/* Action buttons */}
          <div className="space-y-3 pb-safe">
            <Button
              variant="primary"
              size="lg"
              fullWidth
              onClick={() => navigate('/briefs', { replace: true })}
            >
              확인
            </Button>
            <Button
              variant="secondary"
              size="lg"
              fullWidth
              onClick={() => navigate('/consultations/new', { replace: true })}
              className="border border-[#dee1e6]"
            >
              다른 분야 선택
            </Button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1">
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
            <p className="text-xs font-medium text-[#31383f] tracking-[1.2px] uppercase">
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
            <div className="mt-6 bg-[#f3f5f6] border border-[#dde0e4] rounded-pill px-4 py-2 shadow-sm">
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
