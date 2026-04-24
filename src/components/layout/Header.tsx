import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
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
      {/* Left: back button, or SHIELD logo → 홈 이동 on tab roots */}
      <div className="w-10 shrink-0 flex items-center">
        {showBack ? (
          <button
            type="button"
            onClick={onBack}
            aria-label="뒤로 가기"
            className="flex items-center justify-center w-11 h-11 -ml-2 rounded-lg text-gray-600 hover:bg-gray-100 active:bg-gray-200 transition-colors"
          >
            <ArrowLeft size={20} aria-hidden="true" />
          </button>
        ) : (
          <Link
            to="/"
            aria-label="홈으로 이동"
            className="flex items-center justify-center w-11 h-11 -ml-2 rounded-lg hover:bg-gray-100 active:bg-gray-200 transition-colors"
          >
            <img src="/logo.png" alt="SHIELD" className="w-5 h-5 object-contain" />
          </Link>
        )}
      </div>

      {/* Center: title */}
      <h1 className="flex-1 min-w-0 text-center text-base font-semibold text-gray-900 truncate px-2">
        {title}
      </h1>

      {/* Right: action or spacer */}
      <div className="shrink-0 flex items-center justify-end">
        {rightAction}
      </div>
    </header>
  );
}
