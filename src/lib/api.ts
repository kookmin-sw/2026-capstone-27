import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { API_URL } from './constants';
import { getAccessToken, setAccessToken, clearTokens } from './auth';

const api = axios.create({
  baseURL: `${API_URL}/api`,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request Interceptor: JWT 자동 주입 ──
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Response Interceptor: 401 → refresh → retry ──
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else if (token) resolve(token);
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${token}`;
        }
        return api(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // 명세: Body 없이 호출, Refresh Token은 HttpOnly Cookie로 자동 전송
      const { data } = await axios.post(
        `${API_URL}/api/auth/token/refresh`,
        null,
        { withCredentials: true },
      );

      const newAccess = data.data.accessToken as string;
      setAccessToken(newAccess);
      processQueue(null, newAccess);

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${newAccess}`;
      }
      return api(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      clearTokens();
      window.location.href = '/login';
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

export default api;
