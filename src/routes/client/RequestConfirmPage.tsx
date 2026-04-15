import { useNavigate, useParams, useLocation, Link } from 'react-router-dom';
import { CheckCircle } from 'lucide-react';
import { Button } from '@/components/ui';
import { Header } from '@/components/layout/Header';

// ─── page ────────────────────────────────────────────────────────────────────

interface LocationState {
  briefTitle?: string;
  lawyerName?: string;
}

export function RequestConfirmPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  const state = (location.state as LocationState) ?? {};
  const briefTitle = state.briefTitle ?? '의뢰서';
  const lawyerName = state.lawyerName ?? '담당 변호사';

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="요청 완료" showBack={false} />

      <main className="flex-1 flex flex-col items-center justify-center px-6 text-center gap-6 pb-10">
        {/* Success icon */}
        <div
          className="w-24 h-24 rounded-full bg-blue-50 flex items-center justify-center
                     animate-[scale-in_0.4s_cubic-bezier(0.34,1.56,0.64,1)_both]"
          style={{
            animation: 'scaleIn 0.4s cubic-bezier(0.34,1.56,0.64,1) both',
          }}
        >
          <CheckCircle
            size={52}
            className="text-brand"
            strokeWidth={1.8}
            aria-hidden="true"
          />
        </div>

        {/* Title + subtitle */}
        <div className="space-y-2">
          <h1 className="text-xl font-bold text-gray-900">요청이 전송되었습니다</h1>
          <p className="text-sm text-gray-500 leading-relaxed">
            {briefTitle} 의뢰서가
            <br />
            <span className="font-medium text-gray-700">{lawyerName}</span> 변호사에게
            성공적으로 전달되었습니다.
          </p>
        </div>

        {/* Timeline expectation card */}
        <div className="w-full rounded-xl bg-blue-50 px-5 py-4 text-left space-y-1.5">
          <p className="text-xs font-semibold text-blue-800">응답 예상 시간</p>
          <p className="text-sm text-blue-700 leading-relaxed">
            변호사가 <span className="font-semibold">24시간 내</span>에 응답할 예정입니다.
            알림이 도착하면 확인해 주세요.
          </p>
        </div>

        {/* Action buttons */}
        <div className="w-full space-y-3 pt-2">
          <Button
            variant="primary"
            fullWidth
            size="lg"
            onClick={() => navigate(`/briefs/${id}/tracking`)}
          >
            요청 현황 보기
          </Button>

          <Link
            to="/home"
            className="block text-center text-sm text-gray-500 hover:text-gray-700 transition-colors py-1"
          >
            홈으로 돌아가기
          </Link>
        </div>
      </main>

      {/* Inline CSS animation */}
      <style>{`
        @keyframes scaleIn {
          from { opacity: 0; transform: scale(0.6); }
          to   { opacity: 1; transform: scale(1); }
        }
      `}</style>
    </div>
  );
}
