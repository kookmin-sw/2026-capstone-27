import type { OAuthProvider, UserRole } from './enums';

export interface SocialLoginRequest {
  provider: OAuthProvider;
  authorizationCode: string;
  state?: string; // Naver CSRF state
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  isNewUser: boolean;
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

export interface RegisterLawyerRequest {
  name: string;
  email: string;
  phone: string;
  specializations: string[];
  experienceYears: number;
  licenseNumber: string;
}
