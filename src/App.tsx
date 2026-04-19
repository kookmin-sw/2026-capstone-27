import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

// Guards
import { ProtectedRoute } from '@/guards/ProtectedRoute';
import { RoleRoute } from '@/guards/RoleRoute';
import { OnboardingRoute } from '@/guards/OnboardingRoute';

// Error Boundary + Page Loader
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { PageLoader } from '@/components/PageLoader';

// Layouts (loaded eagerly — small files, always needed)
import { AuthLayout } from '@/layouts/AuthLayout';
import { ClientLayout } from '@/layouts/ClientLayout';
import { LawyerLayout } from '@/layouts/LawyerLayout';
import { AdminLayout } from '@/layouts/AdminLayout';

// ── Auth Pages (lazy) ──
const LoginPage = lazy(() =>
  import('@/routes/auth/LoginPage').then((m) => ({ default: m.LoginPage })),
);
const KakaoCallbackPage = lazy(() =>
  import('@/routes/auth/KakaoCallbackPage').then((m) => ({ default: m.KakaoCallbackPage })),
);
const NaverCallbackPage = lazy(() =>
  import('@/routes/auth/NaverCallbackPage').then((m) => ({ default: m.NaverCallbackPage })),
);
const GoogleCallbackPage = lazy(() =>
  import('@/routes/auth/GoogleCallbackPage').then((m) => ({ default: m.GoogleCallbackPage })),
);
const RoleSelectPage = lazy(() =>
  import('@/routes/auth/RoleSelectPage').then((m) => ({ default: m.RoleSelectPage })),
);
const ClientRegisterPage = lazy(() =>
  import('@/routes/auth/ClientRegisterPage').then((m) => ({ default: m.ClientRegisterPage })),
);
const LawyerRegisterPage = lazy(() =>
  import('@/routes/auth/LawyerRegisterPage').then((m) => ({ default: m.LawyerRegisterPage })),
);
const SplashPage = lazy(() =>
  import('@/routes/auth/SplashPage').then((m) => ({ default: m.SplashPage })),
);

// ── Client Pages (lazy) ──
const HomePage = lazy(() =>
  import('@/routes/client/HomePage').then((m) => ({ default: m.HomePage })),
);
const ConsultationListPage = lazy(() =>
  import('@/routes/client/ConsultationListPage').then((m) => ({ default: m.ConsultationListPage })),
);
const NewConsultationPage = lazy(() =>
  import('@/routes/client/NewConsultationPage').then((m) => ({ default: m.NewConsultationPage })),
);
const ChatPage = lazy(() =>
  import('@/routes/client/ChatPage').then((m) => ({ default: m.ChatPage })),
);
const AnalyzingPage = lazy(() =>
  import('@/routes/client/AnalyzingPage').then((m) => ({ default: m.AnalyzingPage })),
);
const BriefListPage = lazy(() =>
  import('@/routes/client/BriefListPage').then((m) => ({ default: m.BriefListPage })),
);
const BriefDetailPage = lazy(() =>
  import('@/routes/client/BriefDetailPage').then((m) => ({ default: m.BriefDetailPage })),
);
const BriefDeliveryPage = lazy(() =>
  import('@/routes/client/BriefDeliveryPage').then((m) => ({ default: m.BriefDeliveryPage })),
);
const LawyerListPage = lazy(() =>
  import('@/routes/client/LawyerListPage').then((m) => ({ default: m.LawyerListPage })),
);
const LawyerProfilePage = lazy(() =>
  import('@/routes/client/LawyerProfilePage').then((m) => ({ default: m.LawyerProfilePage })),
);
const ProfilePage = lazy(() =>
  import('@/routes/client/ProfilePage').then((m) => ({ default: m.ProfilePage })),
);
const PrivacySettingsPage = lazy(() =>
  import('@/routes/client/PrivacySettingsPage').then((m) => ({ default: m.PrivacySettingsPage })),
);
const FinalReviewPage = lazy(() =>
  import('@/routes/client/FinalReviewPage').then((m) => ({ default: m.FinalReviewPage })),
);
const RequestConfirmPage = lazy(() =>
  import('@/routes/client/RequestConfirmPage').then((m) => ({ default: m.RequestConfirmPage })),
);
const RequestTrackingPage = lazy(() =>
  import('@/routes/client/RequestTrackingPage').then((m) => ({ default: m.RequestTrackingPage })),
);

// ── Lawyer Pages (lazy) ──
const LawyerDashboardPage = lazy(() =>
  import('@/routes/lawyer/DashboardPage').then((m) => ({ default: m.DashboardPage })),
);
const InboxPage = lazy(() =>
  import('@/routes/lawyer/InboxPage').then((m) => ({ default: m.InboxPage })),
);
const InboxDetailPage = lazy(() =>
  import('@/routes/lawyer/InboxDetailPage').then((m) => ({ default: m.InboxDetailPage })),
);
const ProfileEditPage = lazy(() =>
  import('@/routes/lawyer/ProfileEditPage').then((m) => ({ default: m.ProfileEditPage })),
);
const VerificationPage = lazy(() =>
  import('@/routes/lawyer/VerificationPage').then((m) => ({ default: m.VerificationPage })),
);
const DocumentsPage = lazy(() =>
  import('@/routes/lawyer/DocumentsPage').then((m) => ({ default: m.DocumentsPage })),
);
const CasesPage = lazy(() =>
  import('@/routes/lawyer/CasesPage').then((m) => ({ default: m.CasesPage })),
);

// ── Admin Pages (lazy) ──
const AdminDashboardPage = lazy(() =>
  import('@/routes/admin/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })),
);
const LawyerPendingPage = lazy(() =>
  import('@/routes/admin/LawyerPendingPage').then((m) => ({ default: m.LawyerPendingPage })),
);
const LawyerReviewPage = lazy(() =>
  import('@/routes/admin/LawyerReviewPage').then((m) => ({ default: m.LawyerReviewPage })),
);
const LogsPage = lazy(() =>
  import('@/routes/admin/LogsPage').then((m) => ({ default: m.LogsPage })),
);
const AdminProfilePage = lazy(() =>
  import('@/routes/admin/AdminProfilePage').then((m) => ({ default: m.AdminProfilePage })),
);

// Lawyer's own profile page
const LawyerMyProfilePage = lazy(() =>
  import('@/routes/lawyer/LawyerProfilePage').then((m) => ({ default: m.LawyerProfilePage })),
);

// ── Root Redirect ──
function RootRedirect() {
  const { isAuthenticated, role } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/splash" replace />;
  if (role === 'LAWYER') return <Navigate to="/lawyer" replace />;
  if (role === 'ADMIN') return <Navigate to="/admin" replace />;
  return <Navigate to="/home" replace />;
}

// ── Not Found ──
function NotFoundPage() {
  return (
    <div className="min-h-dvh flex flex-col items-center justify-center gap-4 px-6 text-center">
      <h1 className="text-7xl font-bold text-gray-200">404</h1>
      <p className="text-gray-500">페이지를 찾을 수 없습니다</p>
      <a
        href="/"
        className="inline-flex items-center rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-white hover:bg-blue-600 transition-colors"
      >
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
    <ErrorBoundary>
      <BrowserRouter>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            {/* ══════ 스플래시 (AuthLayout 외부) ══════ */}
            <Route path="/splash" element={<SplashPage />} />

            {/* ══════ 공개 라우트 (비로그인) ══════ */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/auth/kakao/callback" element={<KakaoCallbackPage />} />
              <Route path="/auth/naver/callback" element={<NaverCallbackPage />} />
              <Route path="/auth/google/callback" element={<GoogleCallbackPage />} />

              {/* 온보딩 전용: 소셜 로그인 직후 중간 단계. 인증되었거나 state.accessToken 이 있을 때만 접근 허용 */}
              <Route element={<OnboardingRoute />}>
                <Route path="/role-select" element={<RoleSelectPage />} />
                <Route path="/register/client" element={<ClientRegisterPage />} />
                <Route path="/register/lawyer" element={<LawyerRegisterPage />} />
              </Route>
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
                  <Route path="/briefs/:id/privacy" element={<PrivacySettingsPage />} />
                  <Route path="/briefs/:id/review" element={<FinalReviewPage />} />
                  <Route path="/briefs/:id/confirm" element={<RequestConfirmPage />} />
                  <Route path="/briefs/:id/tracking" element={<RequestTrackingPage />} />
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
                  <Route path="/lawyer/cases" element={<CasesPage />} />
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
                  <Route path="/admin/profile" element={<AdminProfilePage />} />
                </Route>
              </Route>
            </Route>

            {/* ══════ Fallback ══════ */}
            <Route path="/" element={<RootRedirect />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
