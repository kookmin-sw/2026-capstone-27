import { Outlet } from 'react-router-dom';
import { Home, MessageSquare, FileText, User } from 'lucide-react';
import { BottomNav } from '@/components/layout/BottomNav';
import { SideNav } from '@/components/layout/SideNav';

const CLIENT_TABS = [
  { to: '/home',          icon: Home,          label: '홈'    },
  { to: '/consultations', icon: MessageSquare,  label: '상담'  },
  { to: '/briefs',        icon: FileText,       label: '의뢰서' },
  { to: '/profile',       icon: User,           label: '프로필' },
] as const;

export function ClientLayout() {
  return (
    <div className="h-dvh bg-surface flex flex-col overflow-hidden">
      {/* Desktop sidebar */}
      <SideNav tabs={[...CLIENT_TABS]} />

      {/* Page content — flex-1 so pages can fill remaining space */}
      <main className="flex-1 flex flex-col lg:pl-60 pb-20 lg:pb-0 min-h-0">
        <div className="mx-auto max-w-2xl w-full flex-1 flex flex-col min-h-0">
          <Outlet />
        </div>
      </main>

      {/* Mobile / tablet bottom nav */}
      <BottomNav tabs={[...CLIENT_TABS]} />
    </div>
  );
}
