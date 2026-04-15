import { NavLink } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/cn';

export interface TabItem {
  to: string;
  icon: LucideIcon;
  label: string;
}

interface BottomNavProps {
  tabs: TabItem[];
  className?: string;
}

export function BottomNav({ tabs, className }: BottomNavProps) {
  return (
    <nav
      className={cn(
        'fixed bottom-0 left-0 right-0 z-40',
        'h-16 bg-white border-t border-gray-200',
        'flex justify-around items-center',
        'safe-area-bottom',
        'lg:hidden',
        className,
      )}
    >
      {tabs.map(({ to, icon: Icon, label }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          className={({ isActive }) =>
            cn(
              'flex flex-col items-center justify-center gap-0.5',
              'min-w-[56px] px-2 py-1 text-xs font-medium transition-colors',
              isActive ? 'text-blue-500' : 'text-gray-400',
            )
          }
        >
          {({ isActive }) => (
            <>
              <Icon
                size={22}
                strokeWidth={isActive ? 2.5 : 1.75}
                aria-hidden="true"
              />
              <span>{label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}
