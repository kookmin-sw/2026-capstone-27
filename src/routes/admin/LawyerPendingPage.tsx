import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, ChevronRight, Clock } from 'lucide-react';
import { usePendingLawyers } from '@/hooks/useAdmin';
import { Card, Badge, Spinner, Input, Button } from '@/components/ui';
import type { LawyerDetailResponse } from '@/types/lawyer';

export function LawyerPendingPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const { data, isLoading } = usePendingLawyers(page, 20);

  const filtered = data?.content?.filter((l: LawyerDetailResponse) =>
    l.name.includes(search) || l.email.includes(search),
  );

  if (isLoading) return <Spinner size="lg" text="변호사 목록 불러오는 중..." />;

  return (
    <div className="space-y-4">
      <h1 className="text-lg font-bold text-gray-900">변호사 심사 대기</h1>

      {/* 검색 */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <Input
          placeholder="이름 또는 이메일 검색"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10"
        />
      </div>

      {/* 데스크톱 테이블 (md+) */}
      <div className="hidden md:block">
        <Card padding="none">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 text-left text-gray-500">
                <th className="px-4 py-3 font-medium">이름</th>
                <th className="px-4 py-3 font-medium">이메일</th>
                <th className="px-4 py-3 font-medium">전문 분야</th>
                <th className="px-4 py-3 font-medium">상태</th>
                <th className="px-4 py-3 font-medium" />
              </tr>
            </thead>
            <tbody>
              {filtered?.map((lawyer: LawyerDetailResponse) => (
                <tr
                  key={lawyer.lawyerId}
                  className="border-b border-gray-50 hover:bg-gray-50"
                >
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {lawyer.name}
                  </td>
                  <td className="px-4 py-3 text-gray-500">{lawyer.email}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      {lawyer.specializations.slice(0, 2).map((s) => (
                        <Badge key={s} variant="default" size="sm">
                          {s}
                        </Badge>
                      ))}
                      {lawyer.specializations.length > 2 && (
                        <Badge variant="default" size="sm">
                          +{lawyer.specializations.length - 2}
                        </Badge>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <Badge variant="warning" size="sm">
                      <Clock className="mr-1 h-3 w-3" />
                      대기중
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link
                      to={`/admin/lawyers/${lawyer.lawyerId}`}
                      className="text-brand hover:underline text-sm"
                    >
                      심사하기
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!filtered?.length && (
            <p className="py-10 text-center text-sm text-gray-400">
              심사 대기 중인 변호사가 없습니다
            </p>
          )}
        </Card>
      </div>

      {/* 모바일 카드 (<md) */}
      <div className="md:hidden space-y-2">
        {!filtered?.length ? (
          <Card padding="md">
            <p className="text-center text-sm text-gray-400">
              심사 대기 중인 변호사가 없습니다
            </p>
          </Card>
        ) : (
          filtered.map((lawyer: LawyerDetailResponse) => (
            <Link
              key={lawyer.lawyerId}
              to={`/admin/lawyers/${lawyer.lawyerId}`}
            >
              <Card padding="md">
                <div className="flex items-center justify-between">
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">
                      {lawyer.name}
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5 truncate">
                      {lawyer.email}
                    </p>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {lawyer.specializations.slice(0, 2).map((s) => (
                        <Badge key={s} variant="default" size="sm">
                          {s}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-gray-400 shrink-0 ml-2" />
                </div>
              </Card>
            </Link>
          ))
        )}
      </div>

      {/* 페이지네이션 */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={data.first}
          >
            이전
          </Button>
          <span className="text-sm text-gray-500">
            {data.page + 1} / {data.totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setPage((p) => p + 1)}
            disabled={data.last}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  );
}
