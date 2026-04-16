import { TOKEN_KEY } from './constants';

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

/**
 * 로그인 시 accessToken만 localStorage에 저장.
 * refreshToken은 서버가 HttpOnly Cookie로 설정하므로 JS에서 관리하지 않음.
 */
export function setTokens(accessToken: string): void {
  setAccessToken(accessToken);
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function hasTokens(): boolean {
  return !!getAccessToken();
}
