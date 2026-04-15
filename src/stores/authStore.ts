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
  login: (accessToken: string, refreshToken: string) => Promise<void>;
  logout: () => void;
  setUser: (user: UserInfo) => void;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: getAccessToken(),
  isAuthenticated: false,
  isLoading: true,
  role: null,

  login: async (accessToken, refreshToken) => {
    setTokens(accessToken, refreshToken);
    set({ accessToken, isAuthenticated: true });

    try {
      const { data } = await api.get('/users/me');
      const user = data.data as UserInfo;
      set({ user, role: user.role, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  logout: () => {
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
      // Token invalid or expired — refresh interceptor will handle or clear
      if (!get().isAuthenticated) {
        clearTokens();
        set({ isLoading: false });
      }
    }
  },
}));
