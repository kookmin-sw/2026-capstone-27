import { describe, it, expect } from 'vitest';
import {
  SPECIALIZATION_TREE,
  searchSpecializations,
} from './specializations';

describe('SPECIALIZATION_TREE', () => {
  it('should have 8 top-level categories', () => {
    expect(SPECIALIZATION_TREE).toHaveLength(8);
  });

  it('each level1 should have children (level2)', () => {
    for (const l1 of SPECIALIZATION_TREE) {
      expect(l1.children.length).toBeGreaterThan(0);
    }
  });

  it('each level2 leaf should have name and aliases', () => {
    for (const l1 of SPECIALIZATION_TREE) {
      for (const l2 of l1.children) {
        for (const leaf of l2.children) {
          expect(leaf.name).toBeTruthy();
          expect(Array.isArray(leaf.aliases)).toBe(true);
          expect(leaf.aliases.length).toBeGreaterThan(0);
        }
      }
    }
  });

  it('all leaf names should be unique', () => {
    const names: string[] = [];
    for (const l1 of SPECIALIZATION_TREE) {
      for (const l2 of l1.children) {
        for (const leaf of l2.children) {
          names.push(leaf.name);
        }
      }
    }
    const uniqueNames = new Set(names);
    expect(uniqueNames.size).toBe(names.length);
  });
});

describe('searchSpecializations', () => {
  it('should return empty array for empty query', () => {
    expect(searchSpecializations('')).toEqual([]);
    expect(searchSpecializations('   ')).toEqual([]);
  });

  it('should find results by name', () => {
    const results = searchSpecializations('보증금');
    expect(results.length).toBeGreaterThan(0);
    expect(results.some((r) => r.leaf.name.includes('보증금'))).toBe(true);
  });

  it('should find results by alias', () => {
    const results = searchSpecializations('월급 밀림');
    expect(results.length).toBeGreaterThan(0);
    expect(results[0].leaf.name).toBe('임금체불 및 지급청구');
  });

  it('should include correct path (breadcrumb)', () => {
    const results = searchSpecializations('협의이혼');
    expect(results.length).toBeGreaterThan(0);
    const match = results.find((r) => r.leaf.name === '협의이혼');
    expect(match).toBeDefined();
    expect(match!.path[0]).toBe('이혼·위자료·재산분할');
    expect(match!.path[1]).toBe('이혼 절차');
    expect(match!.path[2]).toBe('협의이혼');
  });

  it('should be case-insensitive', () => {
    const upper = searchSpecializations('PL법');
    const lower = searchSpecializations('pl법');
    expect(upper.length).toBe(lower.length);
  });

  it('should return no results for nonsense query', () => {
    const results = searchSpecializations('xyzqwerty');
    expect(results).toEqual([]);
  });
});
