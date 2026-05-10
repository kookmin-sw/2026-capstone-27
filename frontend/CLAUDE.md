# CLAUDE.md

## Project

SHIELD_FE — AI 법률정보 구조화 플랫폼 프론트엔드. USER/LAWYER/ADMIN 3개 역할, 채팅 기반 상담 → 브리프 생성 → 변호사 매칭.

## Commands

- `pnpm dev` — Vite dev (port 5173, `/api` → `localhost:8080`)
- `pnpm build` — `tsc -b && vite build` (타입체크 게이트)
- `pnpm lint` — ESLint

## Key Rules

- 라우트는 `src/App.tsx`에만 등록. 추가 BrowserRouter/Suspense 금지
- 페이지 파일은 `src/routes/<role>/`에 생성 → `App.tsx`에서 `lazy()` import
- HTTP 클라이언트는 `src/lib/api.ts` 하나만 사용. 새 axios 인스턴스 금지
- 서버 상태는 TanStack Query (`src/hooks/`), UI 상태는 Zustand (`src/stores/`)
- 상태값은 `src/types/enums.ts` + `src/lib/constants.ts` 둘 다 업데이트
- Tailwind v4 (`src/index.css`), 조건부 클래스는 `cn()` from `src/lib/cn.ts`
- import 경로는 `@/*` 사용 (= `src/*`)
- 토큰: localStorage (`shield_access_token`, `shield_refresh_token`)
- OAuth: Kakao/Naver/Google — 콜백은 `src/routes/auth/`

## Workflow

- 코드 변경 후 반드시 `pnpm build` 통과 확인. 타입 에러를 린터/tsconfig 설정 변경으로 우회 금지
- 커밋은 작고 원자적으로: 한 커밋 = 한 논리적 변경. 여러 기능을 하나에 섞지 않기
- 커밋 전 `pnpm lint && pnpm build` 통과 필수
- UI 변경은 dev 서버에서 브라우저로 직접 확인 후 완료 선언

## Status Flows

- Consultation: COLLECTING → ANALYZING → AWAITING_CONFIRM → CONFIRMED | REJECTED
- Brief: DRAFT → CONFIRMED → DELIVERED | DISCARDED
- Domains: CIVIL, CRIMINAL, LABOR, SCHOOL_VIOLENCE

## Deploy

Vercel SPA. 빌드 env: `VITE_API_URL`, `VITE_KAKAO_JS_KEY`, `VITE_NAVER_CLIENT_ID`, `VITE_GOOGLE_CLIENT_ID`
