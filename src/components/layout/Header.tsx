import type { ReactNode } from 'react';
import { ArrowLeft } from 'lucide-react';
import { cn } from '@/lib/cn';

interface HeaderProps {
  title: string;
  showBack?: boolean;
  onBack?: () => void;
  rightAction?: ReactNode;
  className?: string;
}

export function Header({
  title,
  showBack = false,
  onBack,
  rightAction,
  className,
}: HeaderProps) {
  return (
    <header
      className={cn(
        'sticky top-0 z-30',
        'h-14 bg-white border-b border-gray-200',
        'flex items-center px-4',
        'safe-area-top',
        className,
      )}
    >
      {/* Left: back button or spacer */}
      <div className="w-10 flex items-center">
        {showBack && (
          <button
            type="button"
            onClick={onBack}
            aria-label="뒤로 가기"
            className="flex items-center justify-center w-9 h-9 -ml-2 rounded-lg text-gray-600 hover:bg-gray-100 active:bg-gray-200 transition-colors"
          >
            <ArrowLeft size={20} aria-hidden="true" />
          </button>
        )}
      </div>

      {/* Center: title */}
      <h1 className="flex-1 text-center text-base font-semibold text-gray-900 truncate px-2">
        {title}
      </h1>

      {/* Right: action or spacer */}
      <div className="w-10 flex items-center justify-end">
        {rightAction}
      </div>
    </header>
  );
}
