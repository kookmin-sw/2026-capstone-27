import { create } from 'zustand';
import type { UserInfo, UserRole } from '@/types';
import { getAccessToken, setTokens, clearTokens } from '@/lib/auth';
import api from '@/lib/api';

interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  role: UserRole | null;

  // Actions
  login: (accessToken: string) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: UserInfo) => void;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: getAccessToken(),
  isAuthenticated: false,
  isLoading: true,
  role: null,

  login: async (accessToken) => {
    setTokens(accessToken);
    set({ accessToken, isAuthenticated: true });

    try {
      const { data } = await api.get('/users/me');
      const user = data.data as UserInfo;
      set({ user, role: user.role, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  logout: async () => {
    // 명세: POST /api/auth/logout — 서버 세션/RefreshToken 무효화
    try {
      await api.post('/auth/logout');
    } catch {
      // 네트워크 에러 등은 무시하고 로컬 정리 진행
    }
    clearTokens();
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isLoading: false,
      role: null,
    });
  },

  setUser: (user) => {
    set({ user, role: user.role, isAuthenticated: true, isLoading: false });
  },

  initialize: async () => {
    const token = getAccessToken();
    if (!token) {
      set({ isLoading: false });
      return;
    }

    try {
      const { data } = await api.get('/users/me');
      const user = data.data as UserInfo;
      set({
        user,
        accessToken: token,
        isAuthenticated: true,
        role: user.role,
        isLoading: false,
      });
    } catch {
      if (!get().isAuthenticated) {
        clearTokens();
        set({ isLoading: false });
      }
    }
  },
}));
