import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/cn';

type SpinnerSize = 'sm' | 'md' | 'lg';

interface SpinnerProps {
  size?: SpinnerSize;
  text?: string;
  className?: string;
}

const sizeMap: Record<SpinnerSize, number> = {
  sm: 16,
  md: 24,
  lg: 32,
};

export function Spinner({ size = 'md', text, className }: SpinnerProps) {
  const px = sizeMap[size];

  return (
    <div
      className={cn('flex flex-col items-center justify-center gap-2', className)}
      role="status"
      aria-label={text ?? 'Loading'}
    >
      <Loader2
        size={px}
        className="animate-spin text-brand"
        aria-hidden="true"
      />
      {text && (
        <span className="text-sm text-[#64748B] leading-snug">{text}</span>
      )}
    </div>
  );
}
