import api from './api';

export type FcmDeviceType = 'ANDROID' | 'IOS' | 'WEB';

export async function registerFcmToken(token: string, deviceType: FcmDeviceType = 'WEB'): Promise<void> {
  await api.post('/fcm/tokens', { token, deviceType });
}

export async function unregisterFcmToken(token: string): Promise<void> {
  await api.delete('/fcm/tokens', { params: { token } });
}
