import { describe, it, expect, vi, afterEach } from 'vitest';
import { formatDate, formatDateTime, relativeTime } from './dateUtils';

describe('formatDate', () => {
  it('formats ISO string to YYYY.MM.DD', () => {
    expect(formatDate('2026-04-17T14:30:00Z')).toMatch(/^2026\.04\.1[67]$/);
  });

  it('pads single-digit month and day', () => {
    expect(formatDate('2026-01-05T00:00:00Z')).toMatch(/2026\.01\.0[45]/);
  });
});

describe('formatDateTime', () => {
  it('formats ISO string to YYYY.MM.DD HH:MM', () => {
    const result = formatDateTime('2026-04-17T14:30:00Z');
    expect(result).toMatch(/^2026\.04\.\d{2} \d{2}:\d{2}$/);
  });
});

describe('relativeTime', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns empty string for null/undefined', () => {
    expect(relativeTime(null)).toBe('');
    expect(relativeTime(undefined)).toBe('');
  });

  it('returns "방금 전" for less than 60 seconds', () => {
    const now = new Date();
    expect(relativeTime(now.toISOString())).toBe('방금 전');
  });

  it('returns minutes for 1-59 minutes', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString();
    expect(relativeTime(fiveMinAgo)).toBe('5분 전');
  });

  it('returns hours for 1-23 hours', () => {
    const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();
    expect(relativeTime(threeHoursAgo)).toBe('3시간 전');
  });

  it('returns days for 1-29 days', () => {
    const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString();
    expect(relativeTime(twoDaysAgo)).toBe('2일 전');
  });
});
