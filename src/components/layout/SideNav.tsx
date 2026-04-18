import { NavLink } from 'react-router-dom';
import { Shield, User } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/cn';

export interface TabItem {
  to: string;
  icon: LucideIcon;
  label: string;
}

interface SideNavProps {
  tabs: TabItem[];
  className?: string;
}

export function SideNav({ tabs, className }: SideNavProps) {
  return (
    <aside
      className={cn(
        'hidden lg:flex flex-col',
        'fixed left-0 top-0 bottom-0 z-40',
        'w-60 bg-white border-r border-gray-200',
        className,
      )}
    >
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-5 py-5 border-b border-gray-100">
        <div className="flex items-center justify-center w-8 h-8 bg-blue-500 rounded-lg">
          <Shield size={18} className="text-white" aria-hidden="true" />
        </div>
        <span className="text-lg font-bold tracking-tight text-gray-900">
          SHIELD
        </span>
      </div>

      {/* Nav items */}
      <nav className="flex-1 overflow-y-auto py-3 px-3">
        <ul className="flex flex-col gap-0.5" role="list">
          {tabs.map(({ to, icon: Icon, label }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-3 rounded-lg',
                    'text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-blue-50 text-brand'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                  )
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon
                      size={18}
                      strokeWidth={isActive ? 2.5 : 1.75}
                      aria-hidden="true"
                    />
                    <span>{label}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* User profile placeholder */}
      <div className="border-t border-gray-100 px-4 py-4">
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-8 h-8 bg-gray-100 rounded-full">
            <User size={16} className="text-gray-500" aria-hidden="true" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">사용자</p>
            <p className="text-xs text-gray-400 truncate">프로필 보기</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
