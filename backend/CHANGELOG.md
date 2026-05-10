# UI 기반 API 변경 내역 (2026-04-07)

> 프론트엔드 개발자를 위한 API 변경사항 정리

---

## 신규 API (2개)

### 1. 분류 직접 수정
```
PATCH /api/consultations/{consultationId}/classify
```
- AI 분류 신뢰도가 낮을 때 사용자가 직접 분야를 선택하는 API
- 분류 결과 화면에서 "직접 수정" 버튼 클릭 시 호출

```json
// Request
{ "primaryField": "LEASE_DISPUTE" }

// Response
{
  "result": true,
  "message": "분류가 수정되었습니다",
  "data": { "primaryField": "LEASE_DISPUTE" }
}

// primaryField 값
// DEPOSIT_FRAUD / LEASE_DISPUTE / PRESALE / PROPERTY_TRADE / OTHER
```

### 2. 법률 분야 목록 조회 (차후 구현)
```
GET /api/legal-fields
```
- 분야 선택 UI에서 선택지 목록을 가져오는 API
- 현재 차후 구현으로 분류됨

---

## 변경된 API (7개)

### 1. 상담 생성 — Response에 welcomeMessage 추가
```
POST /api/consultations
```

변경 전:
```json
{
  "consultationId": "UUID",
  "status": "CLASSIFYING",
  "createdAt": "DateTime"
}
```

변경 후:
```json
{
  "consultationId": "UUID",
  "status": "CLASSIFYING",
  "welcomeMessage": "반갑습니다. SHIELD 법률 AI입니다. 어떤 법률 문제로 어려움을 겪고 계신가요?",
  "createdAt": "DateTime"
}
```

- 채팅방 입장 시 welcomeMessage를 첫 AI 메시지로 표시
- 백엔드에서 chat_messages에도 자동 저장됨

---

### 2. 법률 분야 분류 — Response에 confidence, tags 추가
```
POST /api/consultations/{consultationId}/classify
```

변경 전:
```json
{ "primaryField": "DEPOSIT_FRAUD" }
```

변경 후:
```json
{
  "primaryField": "DEPOSIT_FRAUD",
  "confidence": "HIGH",
  "tags": ["부동산", "보증금 분쟁"]
}
```

- confidence 값: HIGH / MEDIUM / LOW
  - LOW일 때 "직접 수정" 버튼 표시
- tags: 분류 결과 화면에서 키워드 태그로 표시 (#부동산 #보증금 분쟁)

---

### 3. 변호사 목록 조회 — 필터 추가 + Response 확장
```
GET /api/lawyers
```

Request 변경:
```
변경 전: ?page=0&size=20&specialization=부동산
변경 후: ?page=0&size=20&specialization=부동산&minExperience=5&sort=relevance
```

| 파라미터 | 타입 | 설명 | 필수 |
|---------|------|------|------|
| page | int | 페이지 (기본 0) | X |
| size | int | 페이지 크기 (기본 20) | X |
| specialization | String | 전문 분야 필터 | X |
| minExperience | int | 최소 경력 (년) | X (신규) |
| sort | String | 정렬 기준 (기본 relevance) | X (신규) |

sort 값:
- relevance: 의뢰서 키워드 매칭 많은 순
- experience: 경력 높은 순
- name: 이름 가나다순

Response 변경 — 각 변호사에 tags, matchedKeywords 추가:
```json
{
  "lawyerId": "UUID",
  "name": "김변호사",
  "specializations": "부동산",
  "experienceYears": 12,
  "tags": ["보증금 반환", "전세사기", "임대차 분쟁"],
  "matchedKeywords": ["보증금 반환", "전세사기"]
}
```

- tags: 변호사의 세부 전문 키워드
- matchedKeywords: 내 의뢰서 keywords와 겹치는 키워드 (MATCHED KEYWORDS로 표시)

---

### 4. 변호사 프로필 상세 — Response 대폭 확장
```
GET /api/lawyers/{lawyerId}
```

변경 전:
```json
{
  "lawyerId": "UUID",
  "name": "김변호사",
  "specializations": "부동산",
  "experienceYears": 12,
  "certifications": "변호사 자격증",
  "verificationStatus": "VERIFIED"
}
```

변경 후:
```json
{
  "lawyerId": "UUID",
  "name": "김성민 변호사",
  "profileImageUrl": "https://example.com/profile/kim.jpg",
  "specializations": "부동산",
  "experienceYears": 15,
  "tags": ["보증금 반환", "전세사기", "임대차 분쟁"],
  "matchedKeywords": ["보증금 반환", "전세사기"],
  "certifications": ["대한변호사협회 부동산 전문 변호사", "건설법 연수 수료"],
  "caseCount": 120,
  "bio": "김성민 변호사는 지난 15년간 부동산 및 임대차 분쟁 해결에 집중해 왔습니다.",
  "verificationStatus": "VERIFIED"
}
```

| 필드 | 설명 | 신규 |
|------|------|------|
| profileImageUrl | 프로필 사진 URL (nullable) | O |
| tags | 세부 전문 키워드 배열 | O |
| matchedKeywords | 의뢰서와 매칭된 키워드 | O |
| certifications | 자격증 목록 (배열로 변경) | 변경 |
| caseCount | 수행 사례 수 | O |
| bio | 소개글 (nullable) | O |

---

### 5. 의뢰서 조회 — Response에 keyIssues 추가
```
GET /api/briefs/{briefId}
```

추가된 필드:
```json
{
  "keyIssues": [
    {
      "title": "보증금 반환 의무",
      "description": "임대인의 보증금 반환 의무 및 지연 시 손해배상 책임"
    },
    {
      "title": "대항력 보호",
      "description": "확정일자를 받은 임차인의 대항력 성립 여부"
    },
    {
      "title": "퇴거 요구의 부당성",
      "description": "계약 기간 중 일방적 퇴거 요구의 법적 효력"
    }
  ]
}
```

- AI가 의뢰서 생성 시 핵심 쟁점도 함께 생성
- "구조화된 결과 확인" 화면에서 핵심 쟁점 섹션으로 표시

---

### 6. 의뢰서 수정 — Request에 keyIssues 추가
```
PUT /api/briefs/{briefId}
```

```json
// 수정할 필드만 전송
{
  "title": "String",
  "content": "String",
  "keyIssues": [
    { "title": "String", "description": "String" }
  ],
  "keywords": ["String"],
  "privacySetting": "String",
  "status": "String"
}

// privacySetting: PUBLIC / PARTIAL
// status: CONFIRMED / DISCARDED
```

---

### 7. 전달 현황 조회 — Response에 viewedAt 추가
```
GET /api/briefs/{briefId}/deliveries
```

변경 전:
```json
{
  "deliveryId": "UUID",
  "lawyerName": "김변호사",
  "status": "CONFIRMED",
  "sentAt": "DateTime",
  "respondedAt": "DateTime"
}
```

변경 후:
```json
{
  "deliveryId": "UUID",
  "lawyerName": "김변호사",
  "status": "CONFIRMED",
  "sentAt": "2026-03-28T11:00:00Z",
  "viewedAt": "2026-03-28T13:00:00Z",
  "respondedAt": "2026-03-28T14:00:00Z"
}
```

타임라인 표시 기준:
```
sentAt 있음                          → "의뢰서 전달됨"
viewedAt 있음 + respondedAt null     → "의뢰서 열람 / 검토 진행 중"
respondedAt 있음 + status CONFIRMED  → "수락"
respondedAt 있음 + status REJECTED   → "거절"
```

---

## keywords 형식 변경

아래 API에서 keywords가 문자열 → 배열로 변경됨:

| API | 변경 |
|-----|------|
| 의뢰서 조회 | `"keywords": "String"` → `"keywords": ["String"]` |
| 의뢰서 수정 | `"keywords": "String"` → `"keywords": ["String"]` |
| 수신 의뢰서 상세 | `"keywords": "String"` → `"keywords": ["String"]` |

---

## DB 변경 내역 (백엔드 참고)

### users 테이블

| 컬럼 | 변경 | 타입 | 설명 |
|------|------|------|------|
| profile_image_url | 추가 | TEXT (nullable) | 프로필 사진 URL (Google OAuth 등) |
| created_at, updated_at | 삭제 | - | 사용자 테이블에서 불필요 |

### lawyer_profiles 테이블

| 컬럼 | 변경 | 타입 | 설명 |
|------|------|------|------|
| tags | 추가 | JSONB (DEFAULT '[]') | 세부 전문 키워드 ["보증금 반환", "전세사기"] |
| bio | 추가 | TEXT (nullable) | 변호사 소개글 |
| case_count | 추가 | INTEGER (DEFAULT 0) | 수행 사례 수 |
| certifications | 변경 | TEXT → JSONB (DEFAULT '[]') | 자격증 목록 (배열) |

### consultations 테이블

| 컬럼 | 변경 | 타입 | 설명 |
|------|------|------|------|
| chat_messages | 추가 | JSONB (DEFAULT '[]') | 대화 내역 [{sender, content, timestamp}] |
| tags | 추가 | JSONB (DEFAULT '[]') | AI 분류 시 추출 키워드 |
| primary_field | 변경 | NOT NULL → nullable | 분류 전 null |
| chat_session_id | 삭제 | - | MongoDB 제거로 불필요 |
| confidence | 삭제 | - | API 응답으로만 내려줌, DB 미저장 |

### briefs 테이블

| 컬럼 | 변경 | 타입 | 설명 |
|------|------|------|------|
| key_issues | 추가 | JSONB (DEFAULT '[]') | 핵심 쟁점 [{title, description}] |
| keywords | 변경 | VARCHAR → JSONB (DEFAULT '[]') | 키워드 배열 |

### form_templates 테이블

| 컬럼 | 변경 | 타입 | 설명 |
|------|------|------|------|
| field_type | 삭제 | - | 전부 TEXT 입력이라 불필요 |
| options | 삭제 | - | 선택형 UI 안 쓰므로 불필요 |

### ENUM 변경

| ENUM | 변경 내용 |
|------|----------|
| consultation_status | CLASSIFYING / IN_PROGRESS / COMPLETED / REJECTED |
| brief_status | DRAFT / CONFIRMED / DELIVERED / REJECTED |
| delivery_status | DELIVERED / CONFIRMED / REJECTED |
| privacy_setting | PUBLIC / PARTIAL (PRIVATE 삭제) |
| user_role | USER / LAWYER / ADMIN |
| verification_status | PENDING / VERIFIED / REJECTED |
| collect_method | CHATBOT / FORM |
| field_type | 삭제 (ENUM 자체 삭제) |

### 삭제된 테이블/컬렉션

| 대상 | 이유 |
|------|------|
| MongoDB chatSessions | PostgreSQL JSONB로 대체 |
| MongoDB aiAnalysisLogs | consultation 패키지로 이동 (JPA) |
| classifications 테이블 개념 | consultations.primary_field로 대체 |
| case_structures 테이블 개념 | 삭제 (불필요) |

---

## 참고: 전체 프로세스 흐름

```
1. POST /consultations              → 채팅방 생성 + 환영 메시지
2. POST /classify                   → 사건 유형 분류 (confidence, tags)
   └ PATCH /classify                → 신뢰도 낮으면 직접 수정
3. GET /form                        → 폼 템플릿 로드 + 첫 AI 질문
4. POST /messages (반복)             → 챗봇 대화
5. POST /analyze                    → 의뢰서 생성 (비동기)
6. GET /briefs/{id}                 → 의뢰서 확인 (keyIssues 포함)
7. PUT /briefs/{id}                 → 의뢰서 수정/확정
8. GET /briefs/{id}/matching        → 변호사 매칭
9. POST /briefs/{id}/deliver        → 의뢰서 전달
10. GET /briefs/{id}/deliveries     → 전달 현황 (viewedAt 포함)
```
