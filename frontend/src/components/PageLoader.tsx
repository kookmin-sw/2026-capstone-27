import { Spinner } from '@/components/ui';

export function PageLoader() {
  return (
    <div className="flex min-h-[60dvh] items-center justify-center">
      <Spinner size="lg" text="페이지 로딩 중..." />
    </div>
  );
}
