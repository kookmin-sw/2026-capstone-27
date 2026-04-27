import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, isSupported, type Messaging } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: 'AIzaSyChjdnEuvgAYmcl5ouSicr8T2YuSRXpFzc',
  authDomain: 'capstone-shield.firebaseapp.com',
  projectId: 'capstone-shield',
  storageBucket: 'capstone-shield.firebasestorage.app',
  messagingSenderId: '999340953047',
  appId: '1:999340953047:web:e4d073414bddb1f4f84f92',
};

const VAPID_KEY =
  'BCxvwC3thdlqjKLqFVmoog6x_9O6d3ZewiCSHLG-Uo2QF6G-313u7JapJYn4R7KjGNSa-t0ZB5-oKxzNR6B8uz4';

const app = initializeApp(firebaseConfig);

let messagingInstance: Messaging | null = null;

async function getMessagingInstance(): Promise<Messaging | null> {
  if (messagingInstance) return messagingInstance;
  const supported = await isSupported();
  if (!supported) return null;
  messagingInstance = getMessaging(app);
  return messagingInstance;
}

export async function requestFcmToken(): Promise<string | null> {
  const messaging = await getMessagingInstance();
  if (!messaging) return null;

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return null;

  await navigator.serviceWorker.register('/firebase-messaging-sw.js');
  const registration = await navigator.serviceWorker.ready;
  const token = await getToken(messaging, {
    vapidKey: VAPID_KEY,
    serviceWorkerRegistration: registration,
  });

  return token || null;
}

export async function subscribeForegroundMessages(
  callback: (payload: { title?: string; body?: string; data?: Record<string, string> }) => void,
): Promise<void> {
  const messaging = await getMessagingInstance();
  if (!messaging) return;

  onMessage(messaging, (payload) => {
    callback({
      title: payload.notification?.title,
      body: payload.notification?.body,
      data: payload.data as Record<string, string> | undefined,
    });
  });
}
