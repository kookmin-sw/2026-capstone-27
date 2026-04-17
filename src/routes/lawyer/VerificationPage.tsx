import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ShieldCheck, Clock, Search, AlertCircle, PlusCircle } from 'lucide-react';
import { cn } from '@/lib/cn';
import { useVerificationStatus, useRequestVerification } from '@/hooks/useLawyer';
import { Button, Card, Spinner } from '@/components/ui';
import { Header } from '@/components/layout/Header';
import type { VerificationStatus } from '@/types';

// ─── status config ────────────────────────────────────────────────────────────

interface StatusConfig {
  label: string;
  description: string;
  colorClass: string;
  bgClass: string;
  borderClass: string;
  icon: React.ReactNode;
}

const STATUS_CONFIG: Record<VerificationStatus, StatusConfig> = {
  PENDING: {
    label: '심사 대기 중',
    description: '인증 신청이 접수되었습니다. 심사 결과를 기다려 주세요.',
    colorClass: 'text-yellow-700',
    bgClass: 'bg-yellow-50',
    borderClass: 'border-yellow-200',
    icon: <Clock size={24} className="text-yellow-500" />,
  },
  REVIEWING: {
    label: '심사 진행 중',
    description: '제출하신 서류를 검토 중입니다. 조금만 기다려 주세요.',
    colorClass: 'text-blue-700',
    bgClass: 'bg-blue-50',
    borderClass: 'border-blue-200',
    icon: <Search size={24} className="text-blue-500" />,
  },
  VERIFIED: {
    label: '인증 완료',
    description: '변호사 인증이 완료되었습니다.',
    colorClass: 'text-green-700',
    bgClass: 'bg-green-50',
    borderClass: 'border-green-200',
    icon: <ShieldCheck size={24} className="text-green-500" />,
  },
  REJECTED: {
    label: '심사 반려',
    description: '인증 신청이 반려되었습니다. 사유를 확인 후 재신청해 주세요.',
    colorClass: 'text-red-700',
    bgClass: 'bg-red-50',
    borderClass: 'border-red-200',
    icon: <AlertCircle size={24} className="text-red-500" />,
  },
  SUPPLEMENT_REQUESTED: {
    label: '보충 자료 요청',
    description: '추가 서류 제출이 필요합니다. 서류 관리 페이지에서 제출해 주세요.',
    colorClass: 'text-orange-700',
    bgClass: 'bg-orange-50',
    borderClass: 'border-orange-200',
    icon: <PlusCircle size={24} className="text-orange-500" />,
  },
};

// ─── page ────────────────────────────────────────────────────────────────────

export function VerificationPage() {
  const navigate = useNavigate();
  const { data: verification, isLoading } = useVerificationStatus();
  const requestVerification = useRequestVerification();
  const [applied, setApplied] = useState(false);

  async function handleApply() {
    try {
      await requestVerification.mutateAsync({ barAssociationNumber: '' });
      setApplied(true);
    } catch {
      // 에러는 mutation에서 처리
    }
  }

  const verificationStatus = verification?.status;
  const config = verificationStatus ? STATUS_CONFIG[verificationStatus] : null;

  // Statuses that already have an active application
  const hasActiveApplication =
    verificationStatus === 'PENDING' ||
    verificationStatus === 'REVIEWING' ||
    verificationStatus === 'VERIFIED' ||
    verificationStatus === 'SUPPLEMENT_REQUESTED';

  const canApply = !hasActiveApplication || (verificationStatus as string) === 'REJECTED';

  return (
    <div className="flex flex-col min-h-dvh bg-surface">
      <Header title="인증 신청" showBack onBack={() => navigate(-1)} />

      <main className="flex-1 px-4 py-4 pb-10 space-y-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <Spinner size="lg" />
          </div>
        ) : (
          <>
            {/* Current status card */}
            {config && verificationStatus && (
              <Card
                padding="md"
                className={cn(
                  'border',
                  config.bgClass,
                  config.borderClass,
                )}
              >
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0 mt-0.5">{config.icon}</div>
                  <div className="flex-1 min-w-0">
                    <p className={cn('text-base font-semibold', config.colorClass)}>
                      {config.label}
                    </p>
                    <p className="text-sm text-gray-600 mt-1 leading-relaxed">
                      {config.description}
                    </p>
                  </div>
                </div>
              </Card>
            )}

            {/* Apply button */}
            {canApply && !applied && (
              <Card padding="md">
                <p className="text-sm text-gray-600 mb-4 leading-relaxed">
                  {verificationStatus === 'REJECTED'
                    ? '반려 사유를 확인 후 재신청할 수 있습니다.'
                    : '변호사 인증을 신청하면 의뢰인에게 인증 변호사로 표시됩니다.'}
                </p>
                <Button
                  variant="primary"
                  fullWidth
                  isLoading={requestVerification.isPending}
                  onClick={handleApply}
                  leftIcon={<ShieldCheck size={16} />}
                >
                  인증 신청하기
                </Button>
              </Card>
            )}

            {/* Applied success message */}
            {applied && (
              <div className="px-4 py-3 bg-green-50 border border-green-200 rounded-xl">
                <p className="text-sm text-green-700 font-medium">
                  인증 신청이 접수되었습니다.
                </p>
              </div>
            )}

            {/* Document link */}
            <Card padding="md">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold text-gray-900">서류 제출</p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    인증에 필요한 서류를 제출해 주세요
                  </p>
                </div>
                <Link
                  to="/lawyer/documents"
                  className={cn(
                    'inline-flex items-center px-3 py-2 rounded-xl text-sm font-medium',
                    'bg-brand text-white hover:bg-blue-600 active:bg-blue-700',
                    'transition-colors duration-150',
                  )}
                >
                  서류 제출
                </Link>
              </div>
            </Card>
          </>
        )}
      </main>
    </div>
  );
}
