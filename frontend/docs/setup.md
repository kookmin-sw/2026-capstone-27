# 개발 환경 셋업

## 요구 사항

- Node.js 18+
- pnpm 9+

## 설치

```bash
pnpm install
```

## 환경 변수

`.env` 파일을 프로젝트 루트에 생성:

```env
VITE_API_URL=http://localhost:8080
VITE_KAKAO_JS_KEY=your_kakao_key
VITE_NAVER_CLIENT_ID=your_naver_id
VITE_GOOGLE_CLIENT_ID=your_google_id
VITE_APP_NAME=SHIELD
```

## 개발 서버

```bash
pnpm dev
# → http://localhost:5173
# /api 경로는 http://localhost:8080으로 프록시됨
```

## 빌드

```bash
pnpm build
# tsc -b (타입 체크) + vite build (번들링)
```

## 테스트

```bash
pnpm test        # watch 모드
pnpm test:run    # 1회 실행
```

테스트 스택: Vitest + @testing-library/react + jsdom

## 린트

```bash
pnpm lint
```

ESLint flat config (`eslint.config.js`)

## 배포

Vercel SPA 배포. `vercel.json`이 모든 경로를 `index.html`로 rewrite합니다.

필요한 환경 변수:

- `VITE_API_URL`
- `VITE_KAKAO_JS_KEY`
- `VITE_NAVER_CLIENT_ID`
- `VITE_GOOGLE_CLIENT_ID`
