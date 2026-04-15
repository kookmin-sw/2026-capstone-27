import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/cn';

type CardPadding = 'none' | 'sm' | 'md' | 'lg';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  padding?: CardPadding;
  children?: ReactNode;
}

const paddingClasses: Record<CardPadding, string> = {
  none: '',
  sm: 'p-3',
  md: 'p-5',
  lg: 'p-7',
};

export function Card({
  padding = 'md',
  children,
  className,
  ...rest
}: CardProps) {
  return (
    <div
      {...rest}
      className={cn(
        'bg-white rounded-card shadow-sm',
        paddingClasses[padding],
        className,
      )}
    >
      {children}
    </div>
  );
}
