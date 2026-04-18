import { Link } from 'react-router-dom';
import { Clock, Eye, FileQuestion, CheckSquare, AlertTriangle } from 'lucide-react';
import { useAdminStats, useAdminAlerts, usePendingLawyers } from '@/hooks/useAdmin';
import { Spinner } from '@/components/ui';
import type { LawyerDetailResponse } from '@/types/lawyer';

const STATUS_BADGE: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: 'bg-[#f1f0e8]', text: 'text-[#5f5e5a]', label: '승인 대기' },
  REVIEWING: { bg: 'bg-[#e8f0fc]', text: 'text-[#0c5fa5]', label: '검토 중' },
  SUPPLEMENT_REQUESTED: { bg: 'bg-[#faeeda]', text: 'text-[#854f0b]', label: '보완 요청' },
  VERIFIED: { bg: 'bg-[#eaf3de]', text: 'text-[#3b6e11]', label: '승인 완료' },
  REJECTED: { bg: 'bg-[#fcebeb]', text: 'text-[#a32c2c]', label: '거절' },
};

const AVATAR_COLORS = ['#1a6de0', '#3b6e11', '#854f0b'];

export function AdminDashboardPage() {
  const { data: stats, isLoading: statsLoading } = useAdminStats();
  const { data: alerts, isLoading: alertsLoading } = useAdminAlerts();
  const { data: pendingData } = usePendingLawyers(0, 2);

  if (statsLoading) {
    return <Spinner size="lg" text="로딩 중..." />;
  }

  const statCards = [
    { label: '승인 대기', value: stats?.pendingCount ?? 0, icon: Clock, accent: true, to: '/admin/lawyers' },
    { label: '검토 중', value: stats?.reviewingCount ?? 0, icon: Eye, color: 'text-[#1a6de0]' },
    { label: '보완 요청', value: stats?.supplementRequestedCount ?? 0, icon: FileQuestion, color: 'text-[#854f0b]' },
    { label: '오늘 처리', value: stats?.todayProcessedCount ?? 0, icon: CheckSquare, color: 'text-[#3b6e11]' },
  ];

  const recentLawyers: LawyerDetailResponse[] = pendingData?.content ?? [];

  return (
    <div className="space-y-4">
      {/* 통계 카드 — 2x2 그리드 */}
      <div className="grid grid-cols-2 gap-2.5">
        {statCards.map((s) => {
          const cardClass = s.accent
            ? 'bg-[#1a6de0] text-white rounded-[14px] p-3.5 h-[76px]'
            : 'bg-white border border-[#e9edef] rounded-[14px] p-3.5 h-[76px]';
          const content = (
            <div key={s.label} className={cardClass}>
              <p className={`text-[28px] font-medium leading-none ${s.accent ? 'text-white' : (s.color ?? 'text-[#1a1a1a]')}`}>
                {s.value}
              </p>
              <p className={`text-[11px] mt-2 ${s.accent ? 'text-white/75' : 'text-[#6b7280]'}`}>
                {s.label}
              </p>
            </div>
          );
          return s.to ? (
            <Link key={s.label} to={s.to}>{content}</Link>
          ) : (
            <div key={s.label}>{content}</div>
          );
        })}
      </div>

      {/* 경고 배너 */}
      {!alertsLoading && alerts && (
        <div className="bg-[#fff8e5] border-[0.5px] border-[#f0c06f] rounded-[14px] px-3 py-2.5">
          <p className="text-[12px] font-medium text-[#854f0b] mb-1">
            <AlertTriangle className="inline h-3 w-3 mr-1" />
            빠른 확인 필요
          </p>
          <p className="text-[11px] text-[#854f0b]">
            • 24시간 이상 미처리 {alerts.overdueCount}건
            &nbsp;&nbsp;• 서류 누락 {alerts.missingDocumentCount}건
            &nbsp;&nbsp;• 중복 의심 {alerts.duplicateSuspectCount}건
          </p>
        </div>
      )}

      {/* 최근 신청 */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-[14px] font-medium text-[#1a1a1a]">최근 신청</h2>
          <Link to="/admin/lawyers" className="text-[12px] text-[#1a6de0]">전체 보기 →</Link>
        </div>

        <div className="space-y-2.5">
          {recentLawyers.map((lawyer, idx) => {
            const badge = STATUS_BADGE[lawyer.verificationStatus] ?? STATUS_BADGE.PENDING;
            const avatarColor = AVATAR_COLORS[idx % AVATAR_COLORS.length];
            const initial = lawyer.name.charAt(0);
            return (
              <div
                key={lawyer.lawyerId}
                className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] p-3 relative"
              >
                <div className="flex items-start gap-3">
                  {/* 아바타 */}
                  <div
                    className="w-9 h-9 rounded-full flex items-center justify-center shrink-0"
                    style={{ backgroundColor: avatarColor }}
                  >
                    <span className="text-[14px] font-medium text-white">{initial}</span>
                  </div>
                  {/* 정보 */}
                  <div className="flex-1 min-w-0">
                    <p className="text-[14px] font-medium text-[#1a1a1a]">{lawyer.name}</p>
                    <p className="text-[11px] text-[#adb5b8]">{lawyer.bio || 'lawyer@shield.com'}</p>
                  </div>
                  {/* 상태 배지 */}
                  <span className={`${badge.bg} ${badge.text} text-[11px] font-medium h-[22px] px-3 rounded-[11px] flex items-center shrink-0`}>
                    {badge.label}
                  </span>
                </div>

                {/* 전문분야 태그 + 경력 */}
                <div className="flex items-center justify-between mt-2">
                  <div className="flex items-center gap-1.5">
                    {lawyer.specializations?.split(',').map((spec) => (
                      <span key={spec.trim()} className="bg-[#e8f0fc] text-[#0c447c] text-[10px] h-5 px-2 rounded-[10px] flex items-center">
                        {spec.trim()}
                      </span>
                    ))}
                  </div>
                  <Link
                    to={`/admin/lawyers/${lawyer.lawyerId}`}
                    className="bg-[#1a6de0] text-white text-[11px] font-medium h-[22px] px-3 rounded-[11px] flex items-center"
                  >
                    상세 보기
                  </Link>
                </div>
                <p className="text-[10px] text-[#6b7280] mt-1.5">
                  경력 {lawyer.experienceYears}년
                </p>
              </div>
            );
          })}
          {recentLawyers.length === 0 && (
            <div className="bg-white border-[0.5px] border-[#e9edef] rounded-[14px] p-6 text-center">
              <p className="text-[12px] text-[#6b7280]">최근 신청이 없습니다</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
