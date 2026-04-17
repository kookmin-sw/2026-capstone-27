import { useState } from 'react';
import { FileText, CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { useVerificationLogs } from '@/hooks/useAdmin';
import { formatDate } from '@/lib/dateUtils';
import { Card, Spinner, Badge } from '@/components/ui';

const ACTION_CONFIG: Record<string, { label: string; variant: 'success' | 'danger' | 'warning'; icon: React.ReactNode }> = {
  APPROVED: { label: '승인', variant: 'success', icon: <CheckCircle size={14} /> },
  REJECTED: { label: '거절', variant: 'danger', icon: <AlertCircle size={14} /> },
  SUPPLEMENT_REQUESTED: { label: '보완 요청', variant: 'warning', icon: <RefreshCw size={14} /> },
};

export function LogsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useVerificationLogs(page, 20);

  return (
    <div className="space-y-4">
      <h1 className="text-lg font-bold text-gray-900">처리 이력</h1>

      {isLoading ? (
        <div className="flex items-center justify-center h-48">
          <Spinner size="lg" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <Card padding="md">
          <div className="flex flex-col items-center justify-center py-16 text-gray-400">
            <FileText className="h-12 w-12 mb-3" />
            <p className="text-sm font-medium">처리 이력이 없습니다</p>
          </div>
        </Card>
      ) : (
        <>
          <div className="space-y-2">
            {data.content.map((log) => {
              const config = ACTION_CONFIG[log.action] ?? { label: log.action, variant: 'warning' as const, icon: null };
              return (
                <Card key={log.logId} padding="sm">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-semibold text-gray-900 truncate">
                          {log.lawyerName}
                        </p>
                        <Badge variant={config.variant} size="sm">
                          {config.label}
                        </Badge>
                      </div>
                      {log.reason && (
                        <p className="text-xs text-gray-500 mt-1 line-clamp-2">
                          사유: {log.reason}
                        </p>
                      )}
                      <p className="text-xs text-gray-400 mt-1">
                        처리자: {log.processedBy} · {formatDate(log.processedAt)}
                      </p>
                    </div>
                  </div>
                </Card>
              );
            })}
          </div>

          {/* Pagination */}
          {data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-2">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg disabled:opacity-40"
              >
                이전
              </button>
              <span className="text-xs text-gray-500">
                {page + 1} / {data.totalPages}
              </span>
              <button
                type="button"
                disabled={!data.hasNext}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg disabled:opacity-40"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
