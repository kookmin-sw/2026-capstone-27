import { describe, it, expect } from 'vitest';
import { briefApi } from '../briefApi';

describe('briefApi – BE contract tests', () => {
  describe('getList', () => {
    it('returns paginated BriefSummaryResponse', async () => {
      const { data } = await briefApi.getList();
      const page = data.data;

      expect(page).toHaveProperty('content');
      expect(Array.isArray(page.content)).toBe(true);
      expect(page).toHaveProperty('page');
      expect(page).toHaveProperty('totalElements');
      expect(page).toHaveProperty('hasNext');

      const item = page.content[0];
      expect(item).toHaveProperty('briefId');
      expect(item).toHaveProperty('title');
      expect(item).toHaveProperty('status');
      expect(item).toHaveProperty('createdAt');
    });
  });

  describe('getById', () => {
    it('returns full BriefResponse with keyIssues', async () => {
      const { data } = await briefApi.getById('770e8400-e29b-41d4-a716-446655440002');
      const brief = data.data;

      expect(brief).toHaveProperty('briefId');
      expect(brief).toHaveProperty('title');
      expect(brief).toHaveProperty('legalField');
      expect(brief).toHaveProperty('content');
      expect(brief).toHaveProperty('keyIssues');
      expect(Array.isArray(brief.keyIssues)).toBe(true);
      expect(brief.keyIssues[0]).toHaveProperty('title');
      expect(brief.keyIssues[0]).toHaveProperty('description');
      expect(brief).toHaveProperty('keywords');
      expect(brief).toHaveProperty('strategy');
      expect(brief).toHaveProperty('privacySetting');
      expect(brief).toHaveProperty('status');
      expect(brief).toHaveProperty('createdAt');
    });
  });

  describe('update', () => {
    it('returns { briefId, status, updatedAt } (NOT full BriefResponse)', async () => {
      const { data } = await briefApi.update('770e8400-e29b-41d4-a716-446655440002', {
        title: '수정된 제목',
      });
      const result = data.data;

      expect(result).toHaveProperty('briefId');
      expect(result).toHaveProperty('status');
      expect(result).toHaveProperty('updatedAt');
      // Should NOT have full brief fields
      expect(result).not.toHaveProperty('content');
      expect(result).not.toHaveProperty('keyIssues');
      expect(result).not.toHaveProperty('legalField');
    });
  });

  describe('getRecommendations', () => {
    it('returns PAGINATED MatchingResponse (not bare array)', async () => {
      const { data } = await briefApi.getRecommendations(
        '770e8400-e29b-41d4-a716-446655440002',
      );
      const page = data.data;

      // Critical: recommendations are paginated
      expect(page).toHaveProperty('content');
      expect(Array.isArray(page.content)).toBe(true);
      expect(page).toHaveProperty('page');
      expect(page).toHaveProperty('totalElements');
      expect(page).toHaveProperty('hasNext');

      const lawyer = page.content[0];
      expect(lawyer).toHaveProperty('lawyerId');
      expect(lawyer).toHaveProperty('name');
      expect(lawyer).toHaveProperty('specializations');
      expect(lawyer).toHaveProperty('experienceYears');
      expect(lawyer).toHaveProperty('tags');
      expect(lawyer).toHaveProperty('matchedKeywords');
      expect(lawyer).toHaveProperty('bio');
      expect(lawyer).toHaveProperty('region');
      expect(lawyer).toHaveProperty('score');
      expect(typeof lawyer.score).toBe('number');
    });
  });

  describe('deliver', () => {
    it('returns DeliveryResponse with lawyerId', async () => {
      const { data } = await briefApi.deliver(
        '770e8400-e29b-41d4-a716-446655440002',
        '990e8400-e29b-41d4-a716-446655440004',
      );
      const delivery = data.data;

      expect(delivery).toHaveProperty('deliveryId');
      expect(delivery).toHaveProperty('lawyerId');
      expect(delivery).toHaveProperty('lawyerName');
      expect(delivery).toHaveProperty('status');
      expect(delivery).toHaveProperty('sentAt');
      expect(delivery).toHaveProperty('viewedAt');
      expect(delivery).toHaveProperty('respondedAt');
    });
  });

  describe('getDeliveries', () => {
    it('returns { deliveries: [...] } wrapper', async () => {
      const { data } = await briefApi.getDeliveries(
        '770e8400-e29b-41d4-a716-446655440002',
      );
      const wrapper = data.data;

      expect(wrapper).toHaveProperty('deliveries');
      expect(Array.isArray(wrapper.deliveries)).toBe(true);

      const delivery = wrapper.deliveries[0];
      expect(delivery).toHaveProperty('deliveryId');
      expect(delivery).toHaveProperty('lawyerId');
      expect(delivery).toHaveProperty('sentAt');
    });
  });
});
