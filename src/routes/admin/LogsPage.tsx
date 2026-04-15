import { FileText } from 'lucide-react';
import { Card } from '@/components/ui';

export function LogsPage() {
  return (
    <div className="space-y-4">
      <h1 className="text-lg font-bold text-gray-900">처리 이력</h1>

      <Card padding="md">
        <div className="flex flex-col items-center justify-center py-16 text-gray-400">
          <FileText className="h-12 w-12 mb-3" />
          <p className="text-sm font-medium">처리 이력 기능은 추후 구현 예정입니다</p>
          <p className="text-xs mt-1">변호사 심사 및 관리 이력을 확인할 수 있습니다</p>
        </div>
      </Card>
    </div>
  );
}
