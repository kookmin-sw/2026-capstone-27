import { KAKAO_JS_KEY } from './constants';

declare global {
  interface Window {
    Kakao: {
      init: (key: string) => void;
      isInitialized: () => boolean;
      Auth: {
        authorize: (options: {
          redirectUri: string;
          scope?: string;
        }) => void;
      };
    };
  }
}

export function initKakaoSDK(): void {
  if (typeof window.Kakao !== 'undefined' && !window.Kakao.isInitialized()) {
    if (KAKAO_JS_KEY) {
      window.Kakao.init(KAKAO_JS_KEY);
    }
  }
}

export function loginWithKakao(): void {
  if (typeof window.Kakao === 'undefined') {
    console.warn('Kakao SDK not loaded');
    return;
  }
  window.Kakao.Auth.authorize({
    redirectUri: `${window.location.origin}/auth/kakao/callback`,
    scope: 'profile_nickname,account_email,profile_image',
  });
}
