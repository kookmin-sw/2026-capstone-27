import { Briefcase } from 'lucide-react';
import { Header } from '@/components/layout/Header';

export function CasesPage() {
  return (
    <div className="flex-1 flex flex-col">
      <Header title="진행 중 사건" />
      <main className="flex-1 flex flex-col items-center justify-center px-4 py-4 pb-10">
        <Briefcase className="h-12 w-12 text-gray-300 mb-3" />
        <p className="text-sm text-gray-400">진행 중인 사건이 없습니다</p>
      </main>
    </div>
  );
}
