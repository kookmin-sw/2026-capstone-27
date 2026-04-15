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

// Client Pages (Sprint 2)
import { ConsultationListPage } from '@/routes/client/ConsultationListPage';
import { NewConsultationPage } from '@/routes/client/NewConsultationPage';
import { ChatPage } from '@/routes/client/ChatPage';
import { AnalyzingPage } from '@/routes/client/AnalyzingPage';

// Client Pages (Sprint 3)
import { HomePage } from '@/routes/client/HomePage';
import { BriefListPage } from '@/routes/client/BriefListPage';
import { BriefDetailPage } from '@/routes/client/BriefDetailPage';
import { BriefDeliveryPage } from '@/routes/client/BriefDeliveryPage';
import { LawyerListPage } from '@/routes/client/LawyerListPage';
import { LawyerProfilePage } from '@/routes/client/LawyerProfilePage';

// Client Pages (Sprint 3 cont.)
import { ProfilePage } from '@/routes/client/ProfilePage';

// Lawyer Pages (Sprint 4)
import { DashboardPage as LawyerDashboardPage } from '@/routes/lawyer/DashboardPage';
import { InboxPage } from '@/routes/lawyer/InboxPage';
import { InboxDetailPage } from '@/routes/lawyer/InboxDetailPage';
import { ProfileEditPage } from '@/routes/lawyer/ProfileEditPage';
import { VerificationPage } from '@/routes/lawyer/VerificationPage';
import { DocumentsPage } from '@/routes/lawyer/DocumentsPage';

// Admin Pages (Sprint 4)
import { AdminDashboardPage } from '@/routes/admin/AdminDashboardPage';
import { LawyerPendingPage } from '@/routes/admin/LawyerPendingPage';
import { LawyerReviewPage } from '@/routes/admin/LawyerReviewPage';
import { LogsPage } from '@/routes/admin/LogsPage';

// Lawyer Profile detail (reuse from client)
import { LawyerProfilePage as LawyerMyProfilePage } from '@/routes/client/LawyerProfilePage';

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
              <Route path="/home" element={<HomePage />} />
              <Route path="/consultations" element={<ConsultationListPage />} />
              <Route path="/consultations/new" element={<NewConsultationPage />} />
              <Route path="/consultations/:id" element={<ChatPage />} />
              <Route path="/consultations/:id/analyzing" element={<AnalyzingPage />} />
              <Route path="/briefs" element={<BriefListPage />} />
              <Route path="/briefs/:id" element={<BriefDetailPage />} />
              <Route path="/briefs/:id/delivery" element={<BriefDeliveryPage />} />
              <Route path="/lawyers" element={<LawyerListPage />} />
              <Route path="/lawyers/:id" element={<LawyerProfilePage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
          </Route>

          {/* ── 변호사 (LAWYER) ── */}
          <Route element={<RoleRoute allowedRoles={['LAWYER']} />}>
            <Route element={<LawyerLayout />}>
              <Route path="/lawyer" element={<LawyerDashboardPage />} />
              <Route path="/lawyer/inbox" element={<InboxPage />} />
              <Route path="/lawyer/inbox/:id" element={<InboxDetailPage />} />
              <Route path="/lawyer/profile" element={<LawyerMyProfilePage />} />
              <Route path="/lawyer/profile/edit" element={<ProfileEditPage />} />
              <Route path="/lawyer/verification" element={<VerificationPage />} />
              <Route path="/lawyer/documents" element={<DocumentsPage />} />
            </Route>
          </Route>

          {/* ── 관리자 (ADMIN) ── */}
          <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<AdminDashboardPage />} />
              <Route path="/admin/lawyers" element={<LawyerPendingPage />} />
              <Route path="/admin/lawyers/:id" element={<LawyerReviewPage />} />
              <Route path="/admin/logs" element={<LogsPage />} />
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
