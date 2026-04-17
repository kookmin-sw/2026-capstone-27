import { Link } from 'react-router-dom';
import { Clock, Eye, FileQuestion, CheckSquare, AlertTriangle, ArrowRight } from 'lucide-react';
import { useAdminStats, useAdminAlerts } from '@/hooks/useAdmin';
import { Card, Spinner } from '@/components/ui';

export function AdminDashboardPage() {
  const { data: stats, isLoading: statsLoading } = useAdminStats();
  const { data: alerts, isLoading: alertsLoading } = useAdminAlerts();

  if (statsLoading) {
    return <Spinner size="lg" text="로딩 중..." />;
  }

  const statCards = [
    { label: '승인 대기', value: stats?.pendingCount ?? 0, icon: Clock, accent: true, to: '/admin/lawyers' },
    { label: '검토 중', value: stats?.reviewingCount ?? 0, icon: Eye, color: 'text-[#1a6de0]' },
    { label: '보완 요청', value: stats?.supplementRequestedCount ?? 0, icon: FileQuestion, color: 'text-[#854f0b]' },
    { label: '오늘 처리', value: stats?.todayProcessedCount ?? 0, icon: CheckSquare, color: 'text-[#3b6e11]' },
  ];

  return (
    <div className="space-y-4">
      {/* 통계 카드 — 2x2 그리드 */}
      <div className="grid grid-cols-2 gap-2.5">
        {statCards.map((s) => {
          const cardClass = s.accent
            ? 'bg-[#1a6de0] text-white rounded-[14px] p-4 h-[76px]'
            : 'bg-white border border-[#e9edef] rounded-[14px] p-4 h-[76px]';
          const content = (
            <div key={s.label} className={cardClass}>
              <p className={`text-[28px] font-medium ${s.accent ? 'text-white' : (s.color ?? 'text-[#1a1a1a]')}`}>
                {s.value}
              </p>
              <p className={`text-[11px] ${s.accent ? 'text-white/75' : 'text-[#6b7280]'}`}>
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
        <div className="bg-[#fff8e5] border border-[#f0c06f] rounded-[14px] px-3 py-2.5">
          <p className="text-xs font-medium text-[#854f0b] mb-1">
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
          <h2 className="text-sm font-medium text-[#1a1a1a]">최근 신청</h2>
          <Link to="/admin/lawyers" className="text-xs text-[#1a6de0]">전체 보기 →</Link>
        </div>

        <Card padding="none">
          <Link to="/admin/lawyers" className="flex items-center justify-between px-4 py-3.5 hover:bg-gray-50">
            <span className="text-sm font-medium text-[#1a1a1a]">심사 목록</span>
            <ArrowRight className="h-4 w-4 text-gray-400" />
          </Link>
          <Link to="/admin/logs" className="flex items-center justify-between px-4 py-3.5 hover:bg-gray-50 border-t border-[#e9edef]">
            <span className="text-sm font-medium text-[#1a1a1a]">처리 이력</span>
            <ArrowRight className="h-4 w-4 text-gray-400" />
          </Link>
        </Card>
      </div>
    </div>
  );
}
