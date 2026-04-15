import { useNavigate } from 'react-router-dom';
import { User, ChevronRight, LogOut, Bell, FileText, Shield, Info } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { Button, Card, Badge } from '@/components/ui';

export function ProfilePage() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const settings = [
    { label: '알림 설정', icon: Bell, disabled: true },
    { label: '개인정보 처리방침', icon: Shield, disabled: false },
    { label: '이용약관', icon: FileText, disabled: false },
  ];

  return (
    <div className="space-y-6">
      {/* 프로필 카드 */}
      <Card padding="lg">
        <div className="flex flex-col items-center gap-3">
          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gray-100">
            <User className="h-10 w-10 text-gray-400" />
          </div>
          <div className="text-center">
            <h2 className="text-xl font-bold text-gray-900">
              {user?.name || '사용자'}
            </h2>
            <p className="text-sm text-gray-500 mt-0.5">{user?.email}</p>
            <Badge variant="primary" size="sm" className="mt-2">
              의뢰인
            </Badge>
          </div>
        </div>
      </Card>

      {/* 설정 */}
      <Card padding="none">
        <div className="divide-y divide-gray-100">
          {settings.map((item) => (
            <button
              key={item.label}
              disabled={item.disabled}
              className={`flex w-full items-center justify-between px-5 py-4 text-left transition-colors ${
                item.disabled
                  ? 'opacity-50 cursor-not-allowed'
                  : 'hover:bg-gray-50 cursor-pointer'
              }`}
            >
              <div className="flex items-center gap-3">
                <item.icon className="h-5 w-5 text-gray-400" />
                <span className="text-sm font-medium text-gray-700">
                  {item.label}
                </span>
              </div>
              <ChevronRight className="h-4 w-4 text-gray-400" />
            </button>
          ))}
          {/* 앱 버전 */}
          <div className="flex items-center justify-between px-5 py-4">
            <div className="flex items-center gap-3">
              <Info className="h-5 w-5 text-gray-400" />
              <span className="text-sm font-medium text-gray-700">앱 버전</span>
            </div>
            <span className="text-sm text-gray-400">1.0.0</span>
          </div>
        </div>
      </Card>

      {/* 로그아웃 */}
      <Button variant="danger" fullWidth leftIcon={<LogOut className="h-4 w-4" />} onClick={handleLogout}>
        로그아웃
      </Button>
    </div>
  );
}
