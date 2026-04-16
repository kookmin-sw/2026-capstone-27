import { describe, it, expect } from 'vitest';
import { consultationApi } from '../consultationApi';

describe('consultationApi – BE contract tests', () => {
  describe('getList', () => {
    it('returns paginated ConsultationResponse with .content array', async () => {
      const { data } = await consultationApi.getList();
      const page = data.data;

      expect(page.content).toBeDefined();
      expect(Array.isArray(page.content)).toBe(true);
      expect(page).toHaveProperty('page');
      expect(page).toHaveProperty('size');
      expect(page).toHaveProperty('totalElements');
      expect(page).toHaveProperty('totalPages');
      expect(page).toHaveProperty('hasNext');

      const item = page.content[0];
      expect(item).toHaveProperty('consultationId');
      expect(item).toHaveProperty('status');
      expect(item).toHaveProperty('primaryField');
      expect(item).toHaveProperty('tags');
      expect(item).toHaveProperty('lastMessage');
      expect(item).toHaveProperty('lastMessageAt');
      expect(item).toHaveProperty('createdAt');
      expect(item).toHaveProperty('brief');
      // brief is nullable object with briefId, title, status
      expect(item.brief).toHaveProperty('briefId');
      expect(item.brief).toHaveProperty('title');
      expect(item.brief).toHaveProperty('status');
    });
  });

  describe('create', () => {
    it('returns consultationId and welcomeMessage', async () => {
      const { data } = await consultationApi.create('CIVIL');
      const created = data.data;

      expect(created).toHaveProperty('consultationId');
      expect(created).toHaveProperty('welcomeMessage');
      expect(created).toHaveProperty('status');
      expect(created).toHaveProperty('createdAt');
      expect(typeof created.consultationId).toBe('string');
      expect(typeof created.welcomeMessage).toBe('string');
    });
  });

  describe('sendMessage', () => {
    it('returns 202 with allCompleted and classification', async () => {
      const res = await consultationApi.sendMessage(
        '660e8400-e29b-41d4-a716-446655440001',
        '임대차 관련 문제가 있습니다.',
      );

      expect(res.status).toBe(202);

      const msg = res.data.data;
      expect(msg).toHaveProperty('messageId');
      expect(msg).toHaveProperty('role');
      expect(msg).toHaveProperty('content');
      expect(msg).toHaveProperty('createdAt');
      expect(msg).toHaveProperty('allCompleted');
      expect(typeof msg.allCompleted).toBe('boolean');
      expect(msg).toHaveProperty('classification');
      expect(msg.classification).toHaveProperty('primaryField');
      expect(msg.classification).toHaveProperty('tags');
    });
  });

  describe('getMessages', () => {
    it('returns PAGINATED response (PageResponse<MessageResponse>)', async () => {
      const { data } = await consultationApi.getMessages(
        '660e8400-e29b-41d4-a716-446655440001',
      );
      const page = data.data;

      // Critical: messages are paginated, not a bare array
      expect(page).toHaveProperty('content');
      expect(Array.isArray(page.content)).toBe(true);
      expect(page).toHaveProperty('page');
      expect(page).toHaveProperty('size');
      expect(page).toHaveProperty('totalElements');
      expect(page).toHaveProperty('totalPages');
      expect(page).toHaveProperty('hasNext');

      const msg = page.content[0];
      expect(msg).toHaveProperty('messageId');
      expect(msg).toHaveProperty('role');
      expect(msg).toHaveProperty('content');
      expect(msg).toHaveProperty('createdAt');
      // MessageResponse does NOT have allCompleted or classification
      expect(msg).not.toHaveProperty('allCompleted');
      expect(msg).not.toHaveProperty('classification');
    });
  });

  describe('requestAnalyze', () => {
    it('returns 202 with data: null', async () => {
      const res = await consultationApi.requestAnalyze(
        '660e8400-e29b-41d4-a716-446655440001',
      );

      expect(res.status).toBe(202);
      expect(res.data.data).toBeNull();
      expect(res.data.result).toBe(true);
    });
  });
});
