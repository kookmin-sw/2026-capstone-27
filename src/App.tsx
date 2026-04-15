import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

// Guards
import { ProtectedRoute } from '@/guards/ProtectedRoute';
import { RoleRoute } from '@/guards/RoleRoute';

// Layouts
import { AuthLayout } from '@/layouts/AuthLayout';
import { ClientLayout } from '@/layouts/ClientLayout';
import { LawyerLayout } from '@/layouts/LawyerLayout';
import { AdminLayout } from '@/layouts/AdminLayout';

// Auth Pages
import { LoginPage } from '@/routes/auth/LoginPage';
import { KakaoCallbackPage } from '@/routes/auth/KakaoCallbackPage';
import { NaverCallbackPage } from '@/routes/auth/NaverCallbackPage';
import { GoogleCallbackPage } from '@/routes/auth/GoogleCallbackPage';
import { RoleSelectPage } from '@/routes/auth/RoleSelectPage';
import { ClientRegisterPage } from '@/routes/auth/ClientRegisterPage';
import { LawyerRegisterPage } from '@/routes/auth/LawyerRegisterPage';

// Placeholder pages (Sprint 2+ 에서 구현)
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-gray-400">
      <p className="text-lg font-medium">{title}</p>
      <p className="text-sm mt-1">개발 예정</p>
    </div>
  );
}

// ── Root Redirect ──
function RootRedirect() {
  const { isAuthenticated, role } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (role === 'LAWYER') return <Navigate to="/lawyer" replace />;
  if (role === 'ADMIN') return <Navigate to="/admin" replace />;
  return <Navigate to="/home" replace />;
}

// ── Not Found ──
function NotFoundPage() {
  return (
    <div className="min-h-dvh flex flex-col items-center justify-center gap-4">
      <h1 className="text-6xl font-bold text-gray-300">404</h1>
      <p className="text-gray-500">페이지를 찾을 수 없습니다</p>
      <a href="/" className="text-brand hover:underline">
        홈으로 돌아가기
      </a>
    </div>
  );
}

export default function App() {
  const initialize = useAuthStore((s) => s.initialize);

  useEffect(() => {
    initialize();
  }, [initialize]);

  return (
    <BrowserRouter>
      <Routes>
        {/* ══════ 공개 라우트 (비로그인) ══════ */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/kakao/callback" element={<KakaoCallbackPage />} />
          <Route path="/auth/naver/callback" element={<NaverCallbackPage />} />
          <Route path="/auth/google/callback" element={<GoogleCallbackPage />} />
          <Route path="/role-select" element={<RoleSelectPage />} />
          <Route path="/register/client" element={<ClientRegisterPage />} />
          <Route path="/register/lawyer" element={<LawyerRegisterPage />} />
        </Route>

        {/* ══════ 보호 라우트 ══════ */}
        <Route element={<ProtectedRoute />}>
          {/* ── 의뢰인 (USER) ── */}
          <Route element={<RoleRoute allowedRoles={['USER']} />}>
            <Route element={<ClientLayout />}>
              <Route path="/home" element={<PlaceholderPage title="의뢰인 홈" />} />
              <Route path="/consultations" element={<PlaceholderPage title="상담 목록" />} />
              <Route path="/consultations/new" element={<PlaceholderPage title="새 상담" />} />
              <Route path="/consultations/:id" element={<PlaceholderPage title="AI 채팅" />} />
              <Route path="/consultations/:id/analyzing" element={<PlaceholderPage title="의뢰서 생성 중" />} />
              <Route path="/briefs" element={<PlaceholderPage title="의뢰서 목록" />} />
              <Route path="/briefs/:id" element={<PlaceholderPage title="의뢰서 상세" />} />
              <Route path="/briefs/:id/delivery" element={<PlaceholderPage title="전달 현황" />} />
              <Route path="/lawyers" element={<PlaceholderPage title="변호사 목록" />} />
              <Route path="/lawyers/:id" element={<PlaceholderPage title="변호사 프로필" />} />
              <Route path="/profile" element={<PlaceholderPage title="내 프로필" />} />
            </Route>
          </Route>

          {/* ── 변호사 (LAWYER) ── */}
          <Route element={<RoleRoute allowedRoles={['LAWYER']} />}>
            <Route element={<LawyerLayout />}>
              <Route path="/lawyer" element={<PlaceholderPage title="변호사 대시보드" />} />
              <Route path="/lawyer/inbox" element={<PlaceholderPage title="수신함" />} />
              <Route path="/lawyer/inbox/:id" element={<PlaceholderPage title="의뢰서 상세" />} />
              <Route path="/lawyer/profile" element={<PlaceholderPage title="내 프로필" />} />
              <Route path="/lawyer/profile/edit" element={<PlaceholderPage title="프로필 수정" />} />
              <Route path="/lawyer/verification" element={<PlaceholderPage title="인증 신청" />} />
              <Route path="/lawyer/documents" element={<PlaceholderPage title="서류 관리" />} />
            </Route>
          </Route>

          {/* ── 관리자 (ADMIN) ── */}
          <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<PlaceholderPage title="관리자 대시보드" />} />
              <Route path="/admin/lawyers" element={<PlaceholderPage title="심사 목록" />} />
              <Route path="/admin/lawyers/:id" element={<PlaceholderPage title="심사 상세" />} />
              <Route path="/admin/logs" element={<PlaceholderPage title="처리 이력" />} />
            </Route>
          </Route>
        </Route>

        {/* ══════ Fallback ══════ */}
        <Route path="/" element={<RootRedirect />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
