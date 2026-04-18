import { Outlet } from 'react-router-dom';
import { LayoutDashboard, Inbox, User } from 'lucide-react';
import { BottomNav } from '@/components/layout/BottomNav';
import { SideNav } from '@/components/layout/SideNav';

const LAWYER_TABS = [
  { to: '/lawyer',         icon: LayoutDashboard, label: '대시보드' },
  { to: '/lawyer/inbox',   icon: Inbox,           label: '수신함'   },
  { to: '/lawyer/profile', icon: User,            label: '프로필'   },
] as const;

export function LawyerLayout() {
  return (
    <div className="min-h-dvh bg-surface flex flex-col">
      {/* Desktop sidebar */}
      <SideNav tabs={[...LAWYER_TABS]} />

      {/* Page content — flex-1 so pages can fill remaining space */}
      <main className="flex-1 flex flex-col lg:pl-60 pb-20 lg:pb-0">
        <div className="mx-auto max-w-2xl w-full flex-1 flex flex-col">
          <Outlet />
        </div>
      </main>

      {/* Mobile / tablet bottom nav */}
      <BottomNav tabs={[...LAWYER_TABS]} />
    </div>
  );
}
