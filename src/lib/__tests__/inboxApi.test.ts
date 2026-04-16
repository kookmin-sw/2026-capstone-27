import { describe, it, expect } from 'vitest';
import { inboxApi } from '../inboxApi';

describe('inboxApi – BE contract tests', () => {
  describe('getList', () => {
    it('response has briefTitle (not title), sentAt (not createdAt), briefId', async () => {
      const { data } = await inboxApi.getList();
      const page = data.data;

      expect(page).toHaveProperty('content');
      expect(Array.isArray(page.content)).toBe(true);

      const item = page.content[0];
      // Must have briefTitle, NOT title
      expect(item).toHaveProperty('briefTitle');
      expect(item).not.toHaveProperty('title');
      // Must have sentAt, NOT createdAt
      expect(item).toHaveProperty('sentAt');
      expect(item).not.toHaveProperty('createdAt');
      // Must have briefId
      expect(item).toHaveProperty('briefId');
      expect(item).toHaveProperty('deliveryId');
      expect(item).toHaveProperty('legalField');
      expect(item).toHaveProperty('status');
    });
  });

  describe('getStats', () => {
    it('has confirmed (not accepted)', async () => {
      const { data } = await inboxApi.getStats();
      const stats = data.data;

      expect(stats).toHaveProperty('total');
      expect(stats).toHaveProperty('pending');
      // Must be "confirmed", NOT "accepted"
      expect(stats).toHaveProperty('confirmed');
      expect(stats).not.toHaveProperty('accepted');
      expect(stats).toHaveProperty('rejected');
      expect(typeof stats.total).toBe('number');
      expect(typeof stats.confirmed).toBe('number');
    });
  });

  describe('getById', () => {
    it('has clientEmail (not clientId)', async () => {
      const { data } = await inboxApi.getById('aa0e8400-e29b-41d4-a716-446655440005');
      const detail = data.data;

      expect(detail).toHaveProperty('deliveryId');
      expect(detail).toHaveProperty('briefId');
      expect(detail).toHaveProperty('title');
      expect(detail).toHaveProperty('legalField');
      expect(detail).toHaveProperty('content');
      expect(detail).toHaveProperty('keywords');
      expect(detail).toHaveProperty('keyIssues');
      expect(detail).toHaveProperty('status');
      expect(detail).toHaveProperty('clientName');
      // Must have clientEmail, NOT clientId
      expect(detail).toHaveProperty('clientEmail');
      expect(detail).not.toHaveProperty('clientId');
      expect(detail).toHaveProperty('sentAt');
    });
  });

  describe('updateStatus', () => {
    it('returns deliveryId, status, respondedAt', async () => {
      const { data } = await inboxApi.updateStatus(
        'aa0e8400-e29b-41d4-a716-446655440005',
        'CONFIRMED',
      );
      const result = data.data;

      expect(result).toHaveProperty('deliveryId');
      expect(result).toHaveProperty('status');
      expect(result).toHaveProperty('respondedAt');
    });
  });
});
