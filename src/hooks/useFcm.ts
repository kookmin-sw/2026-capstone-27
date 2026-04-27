import { useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { requestFcmToken, subscribeForegroundMessages } from '@/lib/firebase';
import { registerFcmToken } from '@/lib/fcmApi';

export function useFcm(): void {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    if (!isAuthenticated) return;

    let cancelled = false;

    (async () => {
      try {
        const token = await requestFcmToken();
        if (cancelled || !token) return;

        await registerFcmToken(token, 'WEB');

        await subscribeForegroundMessages(async (payload) => {
          if (Notification.permission !== 'granted') return;
          const reg = await navigator.serviceWorker.ready;
          await reg.showNotification(payload.title || 'SHIELD', {
            body: payload.body,
            icon: '/logo.png',
            badge: '/logo.png',
            data: payload.data || {},
          });
        });
      } catch (error) {
        console.error('FCM 셋업 실패:', error);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated]);
}
