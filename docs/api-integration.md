# API 연동 가이드

## 기본 설정

모든 API 요청은 `src/lib/api.ts`의 Axios 인스턴스를 통해 이루어집니다.

- Base URL: `VITE_API_URL` 환경변수 (기본 `http://localhost:8080`)
- Dev 프록시: Vite가 `/api` 경로를 `http://localhost:8080`으로 프록시

## 인증

### 토큰 관리

- `shield_access_token` / `shield_refresh_token` — localStorage 키
- Axios request interceptor가 모든 요청에 `Authorization: Bearer <token>` 자동 추가
- 401 응답 → refresh 토큰으로 갱신 → 원래 요청 재시도 (큐 방식)

### OAuth 흐름

```
LoginPage → OAuth SDK redirect → Callback 페이지 → authStore.login()
```

지원 프로바이더: Kakao, Naver, Google

### Dev Login

`POST /auth/dev/login` — 테스트용 (email, name, role 직접 지정)

## API 모듈

| 모듈 | 파일 | 엔드포인트 |
| ---- | ---- | ---- |
| 상담 | `consultationApi.ts` | `/api/consultations/*` |
| 의뢰서 | `briefApi.ts` | `/api/briefs/*` |
| 변호사 | `lawyerApi.ts` | `/api/lawyers/*` |
| 수신함 | `inboxApi.ts` | `/api/inbox/*` |
| 관리자 | `adminApi.ts` | `/api/admin/*` |

## TanStack Query 훅

| 훅 | 파일 | 역할 |
| ---- | ---- | ---- |
| useConsultation* | `hooks/useConsultation.ts` | 상담 CRUD, 분석 요청 |
| useBrief* | `hooks/useBrief.ts` | 의뢰서 조회/수정/확정 |
| useLawyer* | `hooks/useLawyer.ts` | 변호사 목록/상세 |
| useInbox* | `hooks/useInbox.ts` | 변호사 수신함 |
| useAdmin* | `hooks/useAdmin.ts` | 관리자 통계/심사 |
| useChat | `hooks/useChat.ts` | 채팅 메시지 (WebSocket/polling) |
| usePolling | `hooks/usePolling.ts` | 범용 폴링 (분석 대기 등) |

## 에러 처리

- API 에러: Axios interceptor에서 401 자동 처리
- refresh 실패 시: `window.location.href = '/login'` (강제 로그아웃)
- 컴포넌트 레벨: TanStack Query `isError` + Zod 폼 검증
