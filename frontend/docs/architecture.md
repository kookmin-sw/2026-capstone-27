# Architecture

## 기술 스택

| 영역 | 기술 |
| ---- | ---- |
| 런타임 | React 19 + TypeScript |
| 번들러 | Vite |
| 스타일 | Tailwind CSS v4 (`@tailwindcss/vite`) |
| 라우팅 | react-router-dom v7 (SPA, CSR) |
| 클라이언트 상태 | Zustand |
| 서버 상태 | TanStack Query v5 |
| 폼 | react-hook-form + zod |
| 아이콘 | lucide-react |
| 테스트 | Vitest + Testing Library |

## 디렉토리 구조

```
src/
  App.tsx              ← 라우트 정의 (single source of truth)
  main.tsx             ← React 진입점, QueryClient, Kakao SDK init
  index.css            ← Tailwind v4 @theme 디자인 토큰

  routes/              ← 페이지 컴포넌트 (역할별 분리)
    auth/              ← 인증 (Login, Register, OAuth callbacks)
    client/            ← 의뢰인 (Home, Consultations, Briefs, Lawyers)
    lawyer/            ← 변호사 (Dashboard, Inbox, Profile)
    admin/             ← 관리자 (Dashboard, LawyerReview, Logs)

  components/
    ui/                ← 재사용 UI 프리미티브 (Button, Input, Badge, Card, Modal, Spinner, SpecializationPicker)
    layout/            ← 레이아웃 (Header, SideNav, BottomNav)
    chat/              ← 채팅 전용 (ChatBubble, ChatInput, TypingIndicator, ClassifyBadge)

  hooks/               ← 커스텀 훅 (TanStack Query 래퍼)
  stores/              ← Zustand 스토어 (authStore, chatStore)
  lib/                 ← 유틸리티 & API 클라이언트
  types/               ← TypeScript 타입 정의
  layouts/             ← 라우트 레이아웃 (AuthLayout, ClientLayout, ...)
  guards/              ← 라우트 가드 (ProtectedRoute, RoleRoute)
  test/                ← 테스트 설정 (setup.ts)
```

## 라우팅 구조

모든 라우트는 `App.tsx`에서 정의. `lazy()` + `<Suspense>` 패턴 사용.

```
/splash                          → SplashPage (no layout)
/login, /role-select, /register  → AuthLayout
/home, /consultations/*, /briefs/*, /lawyers/*, /profile → ClientLayout (USER)
/lawyer/*                        → LawyerLayout (LAWYER)
/admin/*                         → AdminLayout (ADMIN)
```

## 인증 흐름

1. OAuth (Kakao/Naver/Google) → callback 페이지에서 토큰 수신
2. `authStore.login(accessToken)` → localStorage 저장 + `/users/me` 호출
3. Axios interceptor가 모든 요청에 `Authorization: Bearer` 자동 추가
4. 401 → refresh → retry 큐 (동시 401 대응)

## 상태 관리 분리

- **클라이언트 상태** (UI, 인증): Zustand (`authStore`, `chatStore`)
- **서버 상태** (API 데이터): TanStack Query (`useConsultation`, `useBrief`, `useLawyer`, ...)
- **폼 상태**: react-hook-form + zod

## 디자인 토큰

`src/index.css`의 `@theme` 블록에 정의:

- `--color-brand: #3B82F6`
- `--color-surface: #F8FAFC`
- `--radius-card: 12px`
- `--radius-pill: 9999px`
- `--font-family-sans: Pretendard Variable`
