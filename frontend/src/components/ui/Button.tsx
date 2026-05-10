import { Loader2 } from 'lucide-react';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/cn';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'kakao' | 'naver' | 'google';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  fullWidth?: boolean;
  leftIcon?: ReactNode;
  children?: ReactNode;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-brand text-white hover:bg-blue-600 active:bg-blue-700 focus-visible:ring-brand/40',
  secondary:
    'bg-white text-[#1E293B] border border-gray-300 hover:bg-gray-50 active:bg-gray-100 focus-visible:ring-gray-300/50',
  danger:
    'bg-red-500 text-white hover:bg-red-600 active:bg-red-700 focus-visible:ring-red-400/40',
  kakao:
    'bg-kakao text-[#191919] hover:brightness-95 active:brightness-90 focus-visible:ring-kakao/40',
  naver:
    'bg-naver text-white hover:brightness-95 active:brightness-90 focus-visible:ring-naver/40',
  google:
    'bg-white border border-[#DADCE0] text-gray-700 hover:bg-gray-50 active:bg-gray-100 focus-visible:ring-gray-300/50',
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-10 px-4 text-sm gap-2',
  lg: 'h-12 px-6 text-base gap-2.5',
};

export function Button({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled = false,
  fullWidth = false,
  leftIcon,
  children,
  className,
  ...rest
}: ButtonProps) {
  const isDisabled = disabled || isLoading;

  return (
    <button
      {...rest}
      disabled={isDisabled}
      className={cn(
        // Base
        'inline-flex items-center justify-center font-medium rounded-pill',
        'transition-all duration-150 cursor-pointer select-none',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
        // Variant
        variantClasses[variant],
        // Size
        sizeClasses[size],
        // Full width
        fullWidth && 'w-full',
        // Disabled / loading
        isDisabled && 'opacity-50 cursor-not-allowed pointer-events-none',
        className,
      )}
    >
      {isLoading ? (
        <Loader2 size={size === 'sm' ? 14 : size === 'lg' ? 18 : 16} className="animate-spin" />
      ) : (
        leftIcon && <span className="flex-shrink-0">{leftIcon}</span>
      )}
      {children && <span>{children}</span>}
    </button>
  );
}
