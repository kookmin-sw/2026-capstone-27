import { GOOGLE_CLIENT_ID } from './constants';

export function loginWithGoogle(): void {
  if (!GOOGLE_CLIENT_ID) {
    console.warn('Google Client ID not configured');
    return;
  }

  const params = new URLSearchParams({
    client_id: GOOGLE_CLIENT_ID,
    redirect_uri: `${window.location.origin}/auth/google/callback`,
    response_type: 'code',
    scope: 'openid email profile',
    access_type: 'offline',
    prompt: 'consent',
  });

  window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?${params}`;
}
