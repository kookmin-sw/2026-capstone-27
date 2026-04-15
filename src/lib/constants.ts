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
