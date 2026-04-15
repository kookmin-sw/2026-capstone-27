import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import type { UserRole } from '@/types';

interface RoleRouteProps {
  allowedRoles: UserRole[];
  children?: React.ReactNode;
}

export function RoleRoute({ allowedRoles, children }: RoleRouteProps) {
  const { role } = useAuthStore();

  if (!role || !allowedRoles.includes(role)) {
    const fallback =
      role === 'LAWYER' ? '/lawyer' : role === 'ADMIN' ? '/admin' : '/home';
    return <Navigate to={fallback} replace />;
  }

  return children ?? <Outlet />;
}
