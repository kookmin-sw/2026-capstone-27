import { Link } from 'react-router-dom';
import { Users, Briefcase, Clock, MessageSquare, ArrowRight } from 'lucide-react';
import { useAdminStats, useAdminAlerts } from '@/hooks/useAdmin';
import { Card, Badge, Spinner } from '@/components/ui';

export function AdminDashboardPage() {
  const { data: stats, isLoading: statsLoading } = useAdminStats();
  const { data: alerts, isLoading: alertsLoading } = useAdminAlerts();

  if (statsLoading) {
    return <Spinner size="lg" text="로딩 중..." />;
  }

  const statCards = [
    { label: '전체 사용자', value: stats?.totalUsers ?? 0, icon: Users, bg: 'bg-blue-50', color: 'text-blue-600' },
    { label: '전체 변호사', value: stats?.totalLawyers ?? 0, icon: Briefcase, bg: 'bg-purple-50', color: 'text-purple-600' },
    { label: '심사 대기', value: stats?.pendingVerifications ?? 0, icon: Clock, bg: 'bg-yellow-50', color: 'text-yellow-600', to: '/admin/lawyers' },
    { label: '전체 상담', value: stats?.totalConsultations ?? 0, icon: MessageSquare, bg: 'bg-green-50', color: 'text-green-600' },
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
        ) : !alerts?.length ? (
          <Card padding="md">
            <p className="text-center text-sm text-gray-400">현재 알림이 없습니다</p>
          </Card>
        ) : (
          <div className="space-y-2">
            {alerts.map((alert) => (
              <Card key={alert.id} padding="sm">
                <div className="flex items-start justify-between">
                  <div>
                    <Badge variant="warning" size="sm">{alert.type}</Badge>
                    <p className="text-sm text-gray-700 mt-1">{alert.message}</p>
                  </div>
                  <span className="text-xs text-gray-400 shrink-0 ml-2">
                    {new Date(alert.createdAt).toLocaleDateString('ko-KR')}
                  </span>
                </div>
              </Card>
            ))}
          </div>
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
