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
    { label: '심사 대기', value: stats?.pendingCount ?? 0, icon: Clock, bg: 'bg-yellow-50', color: 'text-yellow-600', to: '/admin/lawyers' },
    { label: '심사 중', value: stats?.reviewingCount ?? 0, icon: Eye, bg: 'bg-blue-50', color: 'text-blue-600' },
    { label: '보충 요청', value: stats?.supplementRequestedCount ?? 0, icon: FileQuestion, bg: 'bg-orange-50', color: 'text-orange-600' },
    { label: '오늘 처리', value: stats?.todayProcessedCount ?? 0, icon: CheckSquare, bg: 'bg-green-50', color: 'text-green-600' },
  ];

  return (
    <div className="space-y-6">
      {/* 통계 */}
      <div className="grid grid-cols-2 gap-3">
        {statCards.map((s) => {
          const content = (
            <Card key={s.label} padding="md">
              <div className={`inline-flex rounded-lg p-2 ${s.bg} mb-2`}>
                <s.icon className={`h-5 w-5 ${s.color}`} />
              </div>
              <p className="text-2xl font-bold text-gray-900">{s.value}</p>
              <p className="text-xs text-gray-500 mt-0.5">{s.label}</p>
            </Card>
          );
          return s.to ? (
            <Link key={s.label} to={s.to}>{content}</Link>
          ) : (
            <div key={s.label}>{content}</div>
          );
        })}
      </div>

      {/* 알림 */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">긴급 알림</h2>
        {alertsLoading ? (
          <Spinner size="md" />
        ) : !alerts ? (
          <Card padding="md">
            <p className="text-center text-sm text-gray-400">현재 알림이 없습니다</p>
          </Card>
        ) : (
          <Card padding="md">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-red-500" />
                  <span className="text-sm text-gray-700">기한 초과</span>
                </div>
                <span className="text-sm font-semibold text-gray-900">{alerts.overdueCount}건</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-orange-500" />
                  <span className="text-sm text-gray-700">서류 미비</span>
                </div>
                <span className="text-sm font-semibold text-gray-900">{alerts.missingDocumentCount}건</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-yellow-500" />
                  <span className="text-sm text-gray-700">중복 의심</span>
                </div>
                <span className="text-sm font-semibold text-gray-900">{alerts.duplicateSuspectCount}건</span>
              </div>
            </div>
          </Card>
        )}
      </div>

      {/* 빠른 링크 */}
      <Card padding="none">
        <Link to="/admin/lawyers" className="flex items-center justify-between px-5 py-4 hover:bg-gray-50">
          <span className="text-sm font-medium">심사 목록</span>
          <ArrowRight className="h-4 w-4 text-gray-400" />
        </Link>
        <Link to="/admin/logs" className="flex items-center justify-between px-5 py-4 hover:bg-gray-50 border-t border-gray-100">
          <span className="text-sm font-medium">처리 이력</span>
          <ArrowRight className="h-4 w-4 text-gray-400" />
        </Link>
      </Card>
    </div>
  );
}
