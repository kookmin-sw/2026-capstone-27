import { NAVER_CLIENT_ID } from './constants';

export function loginWithNaver(): void {
  if (!NAVER_CLIENT_ID) {
    console.warn('Naver Client ID not configured');
    return;
  }

  const state = crypto.randomUUID();
  sessionStorage.setItem('naver_oauth_state', state);

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: NAVER_CLIENT_ID,
    redirect_uri: `${window.location.origin}/auth/naver/callback`,
    state,
  });

  window.location.href = `https://nid.naver.com/oauth2.0/authorize?${params}`;
}

export function validateNaverState(state: string | null): boolean {
  const savedState = sessionStorage.getItem('naver_oauth_state');
  sessionStorage.removeItem('naver_oauth_state');
  return !!state && state === savedState;
}
