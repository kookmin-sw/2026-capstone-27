import { useState } from 'react';
import { Outlet, NavLink } from 'react-router-dom';
import { LayoutDashboard, Users, ClipboardList, Menu, X, Shield, User } from 'lucide-react';
import { cn } from '@/lib/cn';

const ADMIN_TABS = [
  { to: '/admin',         icon: LayoutDashboard, label: '대시보드', end: true  },
  { to: '/admin/lawyers', icon: Users,           label: '심사 목록', end: false },
  { to: '/admin/logs',    icon: ClipboardList,   label: '처리 이력', end: false },
  { to: '/admin/profile', icon: User,            label: '프로필',   end: false },
] as const;

function AdminSidebar({ onClose }: { onClose?: () => void }) {
  return (
    <div className="flex flex-col h-full bg-white">
      {/* Logo */}
      <div className="flex items-center justify-between px-5 py-5 border-b border-gray-100">
        <div className="flex items-center gap-2.5">
          <div className="flex items-center justify-center w-8 h-8 bg-blue-500 rounded-lg">
            <Shield size={18} className="text-white" aria-hidden="true" />
          </div>
          <span className="text-lg font-bold tracking-tight text-gray-900">SHIELD</span>
        </div>
        {/* Close button — only on mobile overlay */}
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            aria-label="메뉴 닫기"
            className="flex items-center justify-center w-8 h-8 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors md:hidden"
          >
            <X size={18} aria-hidden="true" />
          </button>
        )}
      </div>

      {/* Nav items */}
      <nav className="flex-1 overflow-y-auto py-3 px-3">
        <ul className="flex flex-col gap-0.5" role="list">
          {ADMIN_TABS.map(({ to, icon: Icon, label, end }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={end}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-lg',
                    'text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-blue-50 text-brand'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                  )
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon size={18} strokeWidth={isActive ? 2.5 : 1.75} aria-hidden="true" />
                    <span>{label}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}

export function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-dvh bg-surface">
      {/* ── Fixed sidebar (md+) ── */}
      <aside className="hidden md:flex flex-col fixed left-0 top-0 bottom-0 z-40 w-60 border-r border-gray-200">
        <AdminSidebar />
      </aside>

      {/* ── Mobile overlay sidebar ── */}
      {/* Backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm md:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Drawer */}
      <aside
        className={cn(
          'fixed left-0 top-0 bottom-0 z-50 w-60 border-r border-gray-200',
          'transform transition-transform duration-200 ease-in-out md:hidden',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <AdminSidebar onClose={() => setSidebarOpen(false)} />
      </aside>

      {/* ── Main area ── */}
      <div className="md:pl-60 flex flex-col min-h-dvh">
        {/* Mobile header with hamburger */}
        <header className="md:hidden sticky top-0 z-30 h-14 bg-white border-b border-gray-200 flex items-center px-4 gap-3 safe-area-top">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            aria-label="메뉴 열기"
            className="flex items-center justify-center w-9 h-9 -ml-1.5 rounded-lg text-gray-600 hover:bg-gray-100 active:bg-gray-200 transition-colors"
          >
            <Menu size={20} aria-hidden="true" />
          </button>
          <div className="flex items-center gap-2">
            <div className="flex items-center justify-center w-6 h-6 bg-blue-500 rounded">
              <Shield size={14} className="text-white" aria-hidden="true" />
            </div>
            <span className="text-base font-bold text-gray-900">SHIELD 관리자</span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 px-4 py-6 sm:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
