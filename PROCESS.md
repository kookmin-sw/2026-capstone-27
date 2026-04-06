# SHIELD 챗봇 프로세스 흐름

> 부동산 법률 상담 플랫폼 - API 기반 전체 흐름

---

## 전체 흐름 요약

```
채팅방 입장 → 첫 메시지 → 분류 → 폼 로드 → 챗봇 대화 → 의뢰서 생성 → 확정 → 변호사 매칭 → 전달
```

---

## 1단계: 채팅방 입장

```
API: POST /api/consultations
Body: 없음

Response:
{
  "consultationId": "UUID",
  "status": "CLASSIFYING"
}
```

| 항목 | 값 |
|------|-----|
| status | CLASSIFYING |
| primary_field | null |
| chat_messages | [] |

---

## 2단계: 사건 유형 분류

```
API: POST /api/consultations/{consultationId}/classify

Request:
{
  "content": "전세보증금을 안 돌려줘요"
}

Response:
{
  "primaryField": "DEPOSIT_FRAUD"
}
```

### 백엔드 내부 동작
1. `content`를 `chat_messages`에 USER 메시지로 저장
2. AI Router(Grok)로 사건 유형 분류
3. `consultations.primary_field`에 분류 결과 저장
4. status: CLASSIFYING → IN_PROGRESS

### 사건 유형 (primaryField)
| 값 | 의미 |
|----|------|
| DEPOSIT_FRAUD | 전세사기 |
| LEASE_DISPUTE | 임대차 분쟁 |
| PRESALE | 분양 계약 |
| PROPERTY_TRADE | 매매/등기 |
| OTHER | 기타 |

### chat_messages 상태
```json
[
  { "sender": "USER", "content": "전세보증금을 안 돌려줘요", "timestamp": "..." }
]
```

---

## 3단계: 폼 템플릿 로드

```
API: GET /api/consultations/{consultationId}/form

Response:
{
  "legalField": "DEPOSIT_FRAUD",
  "fields": [
    { "fieldName": "deposit_amount",   "label": "보증금 금액",      "collectMethod": "CHATBOT" },
    { "fieldName": "contract_period",  "label": "계약 기간",        "collectMethod": "CHATBOT" },
    { "fieldName": "landlord_contact", "label": "임대인 연락 여부",  "collectMethod": "CHATBOT" },
    { "fieldName": "contract_type",    "label": "계약 유형",        "collectMethod": "CHATBOT" },
    { "fieldName": "desired_outcome",  "label": "원하는 해결 방향",  "collectMethod": "CHATBOT" }
  ]
}
```

### 백엔드 내부 동작
1. `primary_field`로 해당 유형 폼 템플릿 조회
2. 첫 번째 CHATBOT 질문을 `chat_messages`에 AI 메시지로 저장
3. 폼 목록 반환

### chat_messages 상태
```json
[
  { "sender": "USER", "content": "전세보증금을 안 돌려줘요", "timestamp": "..." },
  { "sender": "AI",   "content": "보증금 금액이 얼마인가요?", "timestamp": "..." }
]
```

---

## 4단계: 챗봇 대화 (반복)

```
API: POST /api/consultations/{consultationId}/messages

Request:
{
  "content": "2억이요"
}

Response (진행 중):
{
  "userMessage": { "sender": "USER", "content": "2억이요", "timestamp": "..." },
  "aiMessage":   { "sender": "AI", "content": "계약 기간이 얼마나 남았나요?", "timestamp": "..." },
  "formProgress": { "total": 5, "collected": 1, "completed": false }
}

Response (마지막 답변):
{
  "userMessage": { "sender": "USER", "content": "보증금 돌려받고 싶어요", "timestamp": "..." },
  "aiMessage":   { "sender": "AI", "content": "모든 정보가 수집되었습니다.", "timestamp": "..." },
  "formProgress": { "total": 5, "collected": 5, "completed": true }
}
```

### 동작 규칙
- `completed: false` → aiMessage에 다음 질문
- `completed: true` → aiMessage에 마무리 멘트

### 최종 chat_messages 상태
```json
[
  { "sender": "USER", "content": "전세보증금을 안 돌려줘요" },
  { "sender": "AI",   "content": "보증금 금액이 얼마인가요?" },
  { "sender": "USER", "content": "2억이요" },
  { "sender": "AI",   "content": "계약 기간이 얼마나 남았나요?" },
  { "sender": "USER", "content": "3개월이요" },
  { "sender": "AI",   "content": "임대인과 연락이 되나요?" },
  { "sender": "USER", "content": "연락이 안 돼요" },
  { "sender": "AI",   "content": "계약 유형이 어떻게 되나요?" },
  { "sender": "USER", "content": "전세 계약이요" },
  { "sender": "AI",   "content": "원하는 해결 방향이 있나요?" },
  { "sender": "USER", "content": "보증금 돌려받고 싶어요" },
  { "sender": "AI",   "content": "모든 정보가 수집되었습니다." }
]
```

---

## 5단계: 의뢰서 생성 (비동기)

```
API: POST /api/consultations/{consultationId}/analyze
Body: 없음

Response (즉시):
{
  "result": true,
  "message": "의뢰서 생성을 시작했습니다",
  "data": null
}
```

### 백엔드 백그라운드 처리
1. `chat_messages` JSONB에서 대화 내역 조회
2. 누락 정보 검증
3. 도메인 에이전트(Grok) → 의뢰서 생성
4. `briefs` 테이블에 저장
   - `legal_field`: "DEPOSIT_FRAUD"
   - `keywords`: "보증금 미반환, 임대인 연락두절"
   - `status`: DRAFT
5. `consultations.status` → COMPLETED
6. 의뢰인에게 이메일 알림 발송

---

## 6단계: 의뢰서 확인

사용자가 이메일 또는 앱 재접속으로 돌아옴.

```
API: GET /api/consultations
→ status: COMPLETED, brief: { briefId, title, status } 확인

API: GET /api/consultations/{consultationId}/messages
→ 채팅 내역 표시 + 의뢰서 카드 하단 표시

API: GET /api/briefs/{briefId}
→ 의뢰서 전체 내용 조회
```

### 내 상담 목록 Response
```json
{
  "consultationId": "UUID",
  "status": "COMPLETED",
  "primaryField": "DEPOSIT_FRAUD",
  "lastMessage": "모든 정보가 수집되었습니다.",
  "lastMessageAt": "2026-04-06T10:40:00Z",
  "brief": {
    "briefId": "UUID",
    "title": "전세보증금 미반환 관련 의뢰서",
    "status": "DRAFT"
  }
}
```

### 프론트 화면 구성
```
brief가 null  → 채팅방만 표시
brief가 있음  → 채팅방 + 의뢰서 카드 표시
의뢰서 전체 내용은 GET /briefs/{briefId}로 별도 조회
```

---

## 7단계: 의뢰서 확정

```
API: PUT /api/briefs/{briefId}

→ status: DRAFT → CONFIRMED
```

---

## 8단계: 변호사 매칭

```
API: GET /api/briefs/{briefId}/matching

매칭 로직:
  1차: legal_field = "DEPOSIT_FRAUD" → 부동산 전문 변호사 필터링
  2차: keywords = "보증금 미반환, 임대인 연락두절" → 가장 적합한 변호사 1명

Response:
{
  "lawyer": {
    "lawyerId": "UUID",
    "name": "김변호사",
    "primarySpecialization": "부동산",
    "experienceYears": 12,
    "matchedKeywords": ["전세사기", "보증금반환"]
  }
}
```

---

## 9단계: 의뢰서 전달

```
API: POST /api/briefs/{briefId}/deliver

Request:
{
  "lawyerId": "UUID"
}

→ status: DELIVERED
→ 변호사에게 이메일 알림 발송
```

---

## DB 데이터 흐름

```
[consultations]
  id: UUID
  user_id: FK → users
  status: CLASSIFYING → IN_PROGRESS → COMPLETED
  primary_field: null → DEPOSIT_FRAUD
  chat_messages: [] → [USER, AI, USER, AI, ...]
          │
          │ analyze 완료 시
          ▼
[briefs]
  id: UUID
  consultation_id: FK → consultations
  user_id: FK → users
  legal_field: DEPOSIT_FRAUD
  keywords: "보증금 미반환, 임대인 연락두절"
  title: "전세보증금 미반환 관련 의뢰서"
  content: "의뢰인은 서울시 강남구..."
  status: DRAFT → CONFIRMED → DELIVERED
          │
          │ deliver 시
          ▼
[brief_deliveries]
  id: UUID
  brief_id: FK → briefs
  lawyer_id: FK → users
  status: DELIVERED → CONFIRMED / REJECTED
```

---

## 알림 발송 시점

| 시점 | 수신자 | 내용 |
|------|--------|------|
| 의뢰서 생성 완료 | 의뢰인 | "의뢰서가 생성되었습니다. 확인 후 확정해주세요" |
| 의뢰서 전달 | 변호사 | "새 의뢰서가 도착했습니다" |
| 변호사 수락 | 의뢰인 | "변호사가 의뢰를 수락했습니다" |
| 변호사 거절 | 의뢰인 | "변호사가 의뢰를 거절했습니다" |
| 변호사 인증 승인 | 변호사 | "인증이 승인되었습니다" |
| 변호사 인증 거절 | 변호사 | "인증이 거절되었습니다" |

---

## status 흐름도

### consultations.status
```
CLASSIFYING ──→ IN_PROGRESS ──→ COMPLETED
     │                              
     └──→ REJECTED (분류 실패/취소)    
```

### briefs.status
```
DRAFT ──→ CONFIRMED ──→ DELIVERED
                            │
                            └──→ REJECTED (거절)
```

### brief_deliveries.status
```
DELIVERED ──→ CONFIRMED (수락)
     │
     └──→ REJECTED (거절)
```
