importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'AIzaSyChjdnEuvgAYmcl5ouSicr8T2YuSRXpFzc',
  authDomain: 'capstone-shield.firebaseapp.com',
  projectId: 'capstone-shield',
  storageBucket: 'capstone-shield.firebasestorage.app',
  messagingSenderId: '999340953047',
  appId: '1:999340953047:web:e4d073414bddb1f4f84f92',
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title || 'SHIELD';
  const options = {
    body: payload.notification?.body || '',
    icon: '/logo.png',
    badge: '/logo.png',
    data: payload.data || {},
  };
  self.registration.showNotification(title, options);
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const data = event.notification.data || {};
  let url = '/';
  if (data.type === 'DELIVERY_STATUS') {
    url = '/briefs';
  }
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) return client.focus();
      }
      if (self.clients.openWindow) return self.clients.openWindow(url);
    }),
  );
});
