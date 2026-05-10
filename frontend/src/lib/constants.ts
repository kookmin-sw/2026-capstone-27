export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
export const APP_NAME = import.meta.env.VITE_APP_NAME || 'SHIELD';

export const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY;
export const NAVER_CLIENT_ID = import.meta.env.VITE_NAVER_CLIENT_ID;
export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

export const TOKEN_KEY = 'shield_access_token';
export const REFRESH_TOKEN_KEY = 'shield_refresh_token';

export const DOMAIN_LABELS: Record<string, string> = {
  CIVIL: '민사',
  CRIMINAL: '형사',
  LABOR: '노동',
  SCHOOL_VIOLENCE: '학교폭력',
};

export const CONSULTATION_STATUS_LABELS: Record<string, string> = {
  COLLECTING: '상담 진행 중',
  ANALYZING: '의뢰서 생성 중',
  AWAITING_CONFIRM: '확인 대기',
  CONFIRMED: '확정',
  REJECTED: '거부',
};

export const BRIEF_STATUS_LABELS: Record<string, string> = {
  DRAFT: '초안',
  CONFIRMED: '확정',
  DELIVERED: '전달됨',
  DISCARDED: '폐기',
};

// ── Badge variant mappings (import BadgeVariant from '@/components/ui/Badge') ──

import type { BadgeVariant } from '@/components/ui/Badge';

export const CONSULT_STATUS_BADGE: Record<string, BadgeVariant> = {
  COLLECTING: 'primary',
  ANALYZING: 'warning',
  AWAITING_CONFIRM: 'success',
  CONFIRMED: 'success',
  REJECTED: 'danger',
};

export const BRIEF_STATUS_BADGE: Record<string, BadgeVariant> = {
  DRAFT: 'warning',
  CONFIRMED: 'primary',
  DELIVERED: 'success',
  DISCARDED: 'danger',
};

export const DELIVERY_STATUS_BADGE: Record<string, BadgeVariant> = {
  DELIVERED: 'primary',
  CONFIRMED: 'success',
  REJECTED: 'danger',
};

export const DELIVERY_STATUS_LABEL: Record<string, string> = {
  DELIVERED: '대기 중',
  CONFIRMED: '수락',
  REJECTED: '거절',
};

// ── File upload constants ──

export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
export const ACCEPTED_FILE_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];
export const ACCEPTED_FILE_EXTS = ['.pdf', '.jpg', '.jpeg', '.png'];
