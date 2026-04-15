/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_KAKAO_JS_KEY: string;
  readonly VITE_KAKAO_REST_KEY: string;
  readonly VITE_NAVER_CLIENT_ID: string;
  readonly VITE_GOOGLE_CLIENT_ID: string;
  readonly VITE_APP_NAME: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
