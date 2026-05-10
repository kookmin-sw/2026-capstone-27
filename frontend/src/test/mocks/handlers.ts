import { http, HttpResponse } from 'msw';

const BASE = 'http://localhost:8080/api';

// ── Mock Data ──

const mockUser = {
  userId: '550e8400-e29b-41d4-a716-446655440000',
  email: 'test@example.com',
  name: '홍길동',
  role: 'USER',
  provider: 'google',
  profileImageUrl: null,
  phone: null,
};

const mockConsultation = {
  consultationId: '660e8400-e29b-41d4-a716-446655440001',
  status: 'COLLECTING',
  // 3단계 분류 — 사용자 입력값과 AI 분류값을 분리해 내려준다.
  userDomains: ['CIVIL'],
  userSubDomains: ['LEASE'],
  userTags: ['계약분쟁'],
  aiDomains: null,
  aiSubDomains: null,
  aiTags: null,
  lastMessage: '안녕하세요, 상담을 시작합니다.',
  lastMessageAt: '2025-01-15T10:30:00',
  createdAt: '2025-01-15T10:00:00',
  brief: {
    briefId: '770e8400-e29b-41d4-a716-446655440002',
    title: '임대차 분쟁 의뢰서',
    status: 'DRAFT',
  },
};

const mockMessage = {
  messageId: '880e8400-e29b-41d4-a716-446655440003',
  role: 'USER',
  content: '임대차 관련 문제가 있습니다.',
  createdAt: '2025-01-15T10:05:00',
};

const mockBriefSummary = {
  briefId: '770e8400-e29b-41d4-a716-446655440002',
  title: '임대차 분쟁 의뢰서',
  status: 'DRAFT',
  createdAt: '2025-01-15T11:00:00',
};

const mockBrief = {
  briefId: '770e8400-e29b-41d4-a716-446655440002',
  title: '임대차 분쟁 의뢰서',
  legalField: 'CIVIL',
  content: '서울시 강남구 소재 아파트 임대차 계약 관련 분쟁입니다.',
  keyIssues: [
    { title: '보증금 반환 지연', description: '계약 종료 후 3개월째 보증금을 반환받지 못하고 있습니다.' },
  ],
  keywords: ['임대차', '보증금', '계약해지'],
  strategy: '내용증명 발송 후 민사소송 제기',
  privacySetting: 'PUBLIC',
  status: 'DRAFT',
  createdAt: '2025-01-15T11:00:00',
};

const mockMatching = {
  lawyerId: '990e8400-e29b-41d4-a716-446655440004',
  name: '김변호사',
  profileImageUrl: null,
  domains: ['CIVIL'],
  subDomains: ['CIVIL_LEASE'],
  experienceYears: 10,
  tags: ['임대차', '부동산'],
  matchedKeywords: ['임대차', '보증금'],
  bio: '민사 전문 변호사입니다.',
  region: '서울',
  score: 0.95,
};

const mockDelivery = {
  deliveryId: 'aa0e8400-e29b-41d4-a716-446655440005',
  lawyerId: '990e8400-e29b-41d4-a716-446655440004',
  lawyerName: '김변호사',
  status: 'DELIVERED',
  sentAt: '2025-01-16T09:00:00',
  viewedAt: null,
  respondedAt: null,
};

const mockInboxItem = {
  deliveryId: 'aa0e8400-e29b-41d4-a716-446655440005',
  briefId: '770e8400-e29b-41d4-a716-446655440002',
  briefTitle: '임대차 분쟁 의뢰서',
  legalField: 'CIVIL',
  status: 'DELIVERED',
  sentAt: '2025-01-16T09:00:00',
};

const mockInboxDetail = {
  deliveryId: 'aa0e8400-e29b-41d4-a716-446655440005',
  briefId: '770e8400-e29b-41d4-a716-446655440002',
  title: '임대차 분쟁 의뢰서',
  legalField: 'CIVIL',
  content: '서울시 강남구 소재 아파트 임대차 계약 관련 분쟁입니다.',
  keywords: ['임대차', '보증금'],
  keyIssues: [
    { title: '보증금 반환 지연', description: '계약 종료 후 3개월째 보증금을 반환받지 못하고 있습니다.' },
  ],
  status: 'DELIVERED',
  clientName: '홍길동',
  clientEmail: 'test@example.com',
  sentAt: '2025-01-16T09:00:00',
};

// ── Helper: wrap in ApiResponse ──

function ok<T>(data: T) {
  return HttpResponse.json({ result: true, message: '성공', data });
}

function paginated<T>(items: T[], page = 0, size = 20) {
  return {
    content: items,
    page,
    size,
    totalElements: items.length,
    totalPages: 1,
    hasNext: false,
  };
}

// ── Handlers ──

export const handlers = [
  // ── Auth ──
  http.post(`${BASE}/auth/dev/login`, () => {
    return ok({
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      userId: mockUser.userId,
      name: mockUser.name,
      role: mockUser.role,
    });
  }),

  http.post(`${BASE}/auth/token/refresh`, () => {
    return ok({ accessToken: 'new-mock-access-token' });
  }),

  // ── User ──
  http.get(`${BASE}/users/me`, () => {
    return ok(mockUser);
  }),

  // ── Consultation ──
  http.post(`${BASE}/consultations`, () => {
    return ok({
      consultationId: mockConsultation.consultationId,
      status: 'COLLECTING',
      welcomeMessage: '안녕하세요! SHIELD 법률 AI 상담을 시작합니다.',
      createdAt: '2025-01-15T10:00:00',
    });
  }),

  http.get(`${BASE}/consultations`, () => {
    return ok(paginated([mockConsultation]));
  }),

  http.get(`${BASE}/consultations/:id`, ({ params }) => {
    return ok({ ...mockConsultation, consultationId: params.id as string });
  }),

  http.post(`${BASE}/consultations/:id/messages`, () => {
    return HttpResponse.json(
      {
        result: true,
        message: '성공',
        data: {
          messageId: 'bb0e8400-e29b-41d4-a716-446655440006',
          role: 'CHATBOT',
          content: '네, 임대차 관련 문의시군요. 좀 더 자세히 말씀해주세요.',
          createdAt: '2025-01-15T10:06:00',
          allCompleted: false,
          classification: {
            primaryField: ['CIVIL'],
            tags: ['임대차'],
          },
        },
      },
      { status: 202 },
    );
  }),

  http.get(`${BASE}/consultations/:id/messages`, () => {
    return ok(paginated([mockMessage]));
  }),

  http.patch(`${BASE}/consultations/:id/classify`, () => {
    return ok({
      domains: ['CIVIL'],
      subDomains: ['LEASE'],
      tags: ['계약분쟁'],
    });
  }),

  http.post(`${BASE}/consultations/:id/analyze`, () => {
    return HttpResponse.json(
      { result: true, message: '의뢰서 생성이 시작되었습니다', data: null },
      { status: 202 },
    );
  }),

  // ── Brief ──
  http.get(`${BASE}/briefs`, () => {
    return ok(paginated([mockBriefSummary]));
  }),

  http.get(`${BASE}/briefs/:id/lawyer-recommendations`, () => {
    return ok(paginated([mockMatching]));
  }),

  http.get(`${BASE}/briefs/:id/deliveries`, () => {
    return ok({ deliveries: [mockDelivery] });
  }),

  http.get(`${BASE}/briefs/:id`, () => {
    return ok(mockBrief);
  }),

  http.patch(`${BASE}/briefs/:id`, () => {
    return ok({
      briefId: mockBrief.briefId,
      status: 'CONFIRMED',
      updatedAt: '2025-01-16T08:00:00',
    });
  }),

  http.post(`${BASE}/briefs/:id/deliveries`, () => {
    return ok(mockDelivery);
  }),

  // ── Lawyer Inbox ──
  http.get(`${BASE}/lawyer/inbox/stats`, () => {
    return ok({ total: 10, pending: 5, confirmed: 3, rejected: 2 });
  }),

  http.get(`${BASE}/lawyer/inbox/:id`, () => {
    return ok(mockInboxDetail);
  }),

  http.get(`${BASE}/lawyer/inbox`, () => {
    return ok(paginated([mockInboxItem]));
  }),

  http.patch(`${BASE}/lawyer/inbox/:id/status`, () => {
    return ok({
      deliveryId: 'aa0e8400-e29b-41d4-a716-446655440005',
      status: 'CONFIRMED',
      respondedAt: '2025-01-16T10:00:00',
    });
  }),
];
