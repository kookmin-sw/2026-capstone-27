import { describe, it, expect } from 'vitest';
import api from '../api';

describe('authApi – BE contract tests', () => {
  describe('POST /api/auth/dev/login', () => {
    it('returns accessToken, userId, name, role', async () => {
      const { data } = await api.post('/auth/dev/login', {
        email: 'test@example.com',
        name: '홍길동',
        role: 'USER',
      });
      const login = data.data;

      expect(login).toHaveProperty('accessToken');
      expect(login).toHaveProperty('userId');
      expect(login).toHaveProperty('name');
      expect(login).toHaveProperty('role');
      expect(typeof login.accessToken).toBe('string');
      expect(typeof login.userId).toBe('string');
      expect(typeof login.name).toBe('string');
      expect(typeof login.role).toBe('string');
    });
  });
});
