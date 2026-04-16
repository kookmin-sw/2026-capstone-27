# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SHIELD_FE — frontend for an AI legal-information-structuring platform. Connects three user roles (USER/의뢰인, LAWYER/변호사, ADMIN/관리자) through a chat-driven consultation flow that produces structured briefs and matches them to lawyers. Backend is a separate service consumed via `/api` on `localhost:8080` (proxied in dev).

## Commands

Package manager is **pnpm** (enforced by `vercel.json`).

- `pnpm dev` — Vite dev server on port 5173 with `/api` proxy to `http://localhost:8080`
- `pnpm build` — `tsc -b && vite build` (type-check gate is part of build; failing types fail the build)
- `pnpm lint` — ESLint flat config (`eslint.config.js`); ignores `dist/`
- `pnpm preview` — serve the production build locally

There is no test runner configured.

## Architecture

### Role-based routing (single source of truth: `src/App.tsx`)

All route definitions live in `App.tsx`. Every page except `SplashPage` is `lazy()`-loaded and wrapped in a single top-level `<Suspense fallback={<PageLoader />}>`. The route tree is nested:

```
<ErrorBoundary> → <BrowserRouter> → <Suspense>
  /splash                                      (no layout)
  <AuthLayout>       → public auth & OAuth callbacks
  <ProtectedRoute>   → requires isAuthenticated
    <RoleRoute allowedRoles=['USER']>   → <ClientLayout>  → /home, /consultations/*, /briefs/*, /lawyers/*, /profile
    <RoleRoute allowedRoles=['LAWYER']> → <LawyerLayout>  → /lawyer/*
    <RoleRoute allowedRoles=['ADMIN']>  → <AdminLayout>   → /admin/*
  /                  → <RootRedirect> (role-aware)
  *                  → NotFoundPage
```

When adding a page: put it under `src/routes/<role>/`, export a named component, and register it in `App.tsx` with a `lazy(() => import(...).then(m => ({ default: m.Name })))` wrapper inside the correct role's `<RoleRoute>` block. Do not introduce additional `BrowserRouter`s or top-level `Suspense` boundaries.

`RootRedirect` (in `App.tsx`) and `RoleRoute` both decide destinations based on `role`: `LAWYER`→`/lawyer`, `ADMIN`→`/admin`, else `/home`. Keep these two in sync if role homes change.

### Auth & API client

- **Tokens**: access + refresh stored in `localStorage` under keys from `src/lib/constants.ts` (`shield_access_token`, `shield_refresh_token`). Helpers in `src/lib/auth.ts`.
- **Axios instance**: `src/lib/api.ts` is the only HTTP client. It auto-injects `Authorization: Bearer <token>` and implements a **401 → refresh → retry** flow with a queue so concurrent 401s share a single refresh. On refresh failure it clears tokens and `window.location.href = '/login'`. All feature API modules (`briefApi.ts`, `consultationApi.ts`, `lawyerApi.ts`, `adminApi.ts`, `inboxApi.ts`) import this shared instance — do not create new axios instances.
- **Auth store**: `src/stores/authStore.ts` (Zustand). `initialize()` is called once from `App.tsx` on mount — it reads the token from storage and fetches `/users/me` to hydrate `user`/`role`. `login(access, refresh)` persists tokens and fetches `/users/me`. `isLoading` gates `ProtectedRoute` so guards don't redirect during hydration.
- **OAuth**: Kakao (`src/lib/kakao.ts`, SDK init in `main.tsx`), Naver (`src/lib/naver.ts`), Google (`src/lib/google.ts`). Each has a matching callback page under `src/routes/auth/`.
- **Dev login**: `POST /auth/dev/login` with `{email, name, role}` — used by the Dev Login panel in `LoginPage.tsx` to bypass OAuth when testing as USER/LAWYER/ADMIN.

### State management split

- **Client/UI state** → Zustand (`src/stores/`): `authStore`, `chatStore`.
- **Server state** → TanStack Query (`QueryClient` in `main.tsx`, defaults: `retry: 1`, `staleTime: 5min`, `refetchOnWindowFocus: false`). Feature hooks live in `src/hooks/` (`useBrief`, `useConsultation`, `useLawyer`, `useAdmin`, `useInbox`, `useChat`).
- **Polling**: `src/hooks/usePolling.ts` — generic poller with `shouldStop`, `maxDuration`, `onComplete`, `onTimeout`. Used for async flows like consultation `ANALYZING` → brief ready.

### Domain enums

Status strings are union literals, not enums, in `src/types/enums.ts`. Matching human-readable labels are in `src/lib/constants.ts` (`DOMAIN_LABELS`, `CONSULTATION_STATUS_LABELS`, `BRIEF_STATUS_LABELS`). When adding a new status value, update both files.

Key flows:
- Consultation: `COLLECTING` → `ANALYZING` → `AWAITING_CONFIRM` → `CONFIRMED` | `REJECTED`
- Brief: `DRAFT` → `CONFIRMED` → `DELIVERED` | `DISCARDED`
- Domains: `CIVIL`, `CRIMINAL`, `LABOR`, `SCHOOL_VIOLENCE`

### Styling

- **Tailwind v4** via `@tailwindcss/vite` plugin (no `tailwind.config.js`; config lives in `src/index.css`). Custom color token `brand` is referenced across the app.
- Use `cn()` from `src/lib/cn.ts` (`clsx` + `tailwind-merge`) for conditional classes.
- Shared UI primitives: `src/components/ui/` (`Button`, `Input`, `Card`, `Badge`, `Spinner`, `Modal`). Import from the `@/components/ui` barrel.

### Path alias

`@/*` → `src/*` (configured in both `tsconfig.json` and `vite.config.ts`). Prefer `@/` imports over relative paths that cross directories.

### Deployment

Vercel SPA rewrite (`vercel.json`) sends all paths to `index.html`. Env vars required at build time: `VITE_API_URL`, `VITE_KAKAO_JS_KEY`, `VITE_NAVER_CLIENT_ID`, `VITE_GOOGLE_CLIENT_ID`, optionally `VITE_APP_NAME`.
