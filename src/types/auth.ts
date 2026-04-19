import type { OAuthProvider, UserRole } from './enums';

export interface SocialLoginRequest {
  provider: OAuthProvider;
  authorizationCode: string;
  state?: string; // Naver CSRF state
}

/**
 * POST /api/auth/google, /api/auth/dev/login 응답 data
 *
 * 서버 명세(API 명세서 기준):
 *   - accessToken, userId, name, role 필수
 *   - refreshToken은 HttpOnly 쿠키로 내려오므로 body에는 없을 수 있음
 *   - isNewUser는 최초 가입 시 true — 프론트에서 역할 선택 화면으로 분기하기 위해 사용
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  userId?: string;
  name?: string;
  email?: string;
  isNewUser?: boolean;
  role?: UserRole;
  user?: UserInfo;
}

export interface UserInfo {
  userId: string;
  email: string;
  name: string;
  profileImageUrl?: string;
  role: UserRole;
}

export interface RegisterClientRequest {
  name: string;
  email: string;
  phone: string;
}

/**
 * POST /api/lawyers/me/register 요청
 * API 명세서 기준 필드 (온톨로지 L1/L2/L3 + 기본 정보)
 */
export interface RegisterLawyerRequest {
  barAssociationNumber: string;     // 대한변호사협회 등록번호 (필수)
  domains?: string[];                // 대분류 (온톨로지 L1)
  subDomains?: string[];             // 중분류 (온톨로지 L2)
  tags?: string[];                   // 소분류 (온톨로지 L3)
  experienceYears?: number;          // 경력 연수
  bio?: string;                      // 자기소개
  region?: string;                   // 활동 지역
}

/**
 * POST /api/lawyers/me/register 응답 data
 *
 * 서버 처리:
 *   1) 호출자 권한 체크: authenticated() (USER 상태에서도 호출 가능)
 *   2) User.role → LAWYER 로 승격
 *   3) LawyerProfile 생성, verificationStatus = PENDING
 *   4) role 이 바뀌었으므로 새 JWT(accessToken) 를 재발급하여 응답 포함
 *
 * 프론트는 accessToken 을 받아 useAuthStore.login(accessToken) 으로 교체해야
 * 이후 변호사 전용 API 접근이 가능.
 */
export interface RegisterLawyerResponse {
  verificationStatus: 'PENDING' | 'REVIEWING' | 'SUPPLEMENT_REQUESTED' | 'VERIFIED' | 'REJECTED';
  /**
   * 역할 승격(USER → LAWYER) 이후 재발급된 JWT.
   * 기존 토큰은 role=USER 로 서명되어 있어 변호사 API 호출 시 권한 부족.
   */
  accessToken?: string;
}

/** POST /api/auth/dev/login 요청 */
export interface DevLoginRequest {
  email: string;
  name: string;
  role: UserRole;
}

/** POST /api/auth/google 요청 */
export interface GoogleLoginRequest {
  authorizationCode: string;
  role?: UserRole;
}

/** POST /api/auth/token/refresh 응답 data */
export interface RefreshTokenResponse {
  newAccessToken: string | null;
}
