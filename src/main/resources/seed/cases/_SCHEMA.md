# 판례 시드 JSON 스펙 (Phase C-3)

Phase C-4 인제스트 파이프라인이 소비하는 판례 시드 파일의 포맷 정의.

## 파일 배치
- 디렉터리: `src/main/resources/seed/cases/`
- 파일명: `<case-slug>.json`
  - slug는 사건번호에서 영숫자 추출 + 법원 축약. 예: `2004da26133-대법원.json`
  - 단, 파일시스템 호환성을 위해 한글 대신 로마자 축약(`-sc`, `-seoul-high` 등) 사용 권장
- 한 파일 = 한 심급 판결. 복수 심급을 기록할 때는 별도 파일.

## 스키마

```json
{
  "meta": {
    "source": "law.go.kr",
    "source_id": "<법제처 판례일련번호>",
    "source_url": "https://www.law.go.kr/...",
    "fetched_at": "2026-04-19T12:00:00Z"
  },
  "case": {
    "case_no": "2004다26133",
    "court": "대법원",
    "case_name": "건물명도 등",
    "decision_date": "2005-03-25",
    "case_type": "민사",
    "judgment_type": "판결",
    "disposition": "상고기각",

    "headnote": "판시사항 본문...",
    "holding": "판결요지 본문...",
    "reasoning": "판결이유 본문(선택)...",
    "full_text": "원문 전체(선택)...",

    "cited_articles": [
      "주택임대차보호법 제3조",
      "주택임대차보호법 제3조의2",
      "민법 제621조"
    ],
    "cited_cases": [
      "대법원 2002. 11. 8. 선고 2002다38361 판결"
    ],
    "category_ids": [
      "cat-real-estate-lease"
    ]
  }
}
```

## 필드 규칙

### 필수 (NOT NULL in DB)
- `case.case_no` — 사건번호. 한국 법원 표준 표기 그대로 (예: `2020다12345`, `2019나56789`).
- `case.court` — 법원명. 대법원 / 서울고등법원 / 서울중앙지방법원 등 정식명.
- `case.decision_date` — `YYYY-MM-DD` ISO 포맷.
- `case.case_type` — 열거형: `민사` | `형사` | `가사` | `행정` | `특허` | `헌법` | `기타`.
- `meta.source` — 기본 `law.go.kr`.

### 권장
- `case.headnote`, `case.holding` — 둘 중 적어도 하나는 있어야 임베딩 품질이 확보된다. 인제스트 파이프라인은 `headnote + "\n" + holding`을 임베딩 입력 텍스트로 사용.
- `case.cited_articles` — 참조 법령·조문 배열. 검색 시 `legal_chunks`로 역링크 가능.
- `case.category_ids` — 온톨로지 카테고리. `legal_chunks.category_ids`와 동일 체계 사용하여 카테고리 필터가 판례·법령에 공통 적용.

### 선택
- `case.case_name` — 사건명. BM25 인덱싱 대상.
- `case.reasoning`, `case.full_text` — 본문. 길이 제한 없음. BM25 인덱싱은 `reasoning`만 포함(full_text는 노이즈 방지 위해 제외).
- `case.disposition` — 주문 요약 (상고기각, 파기환송 등).
- `case.cited_cases` — 참조 판례 사건번호 배열.

## 자연키 충돌
DB 자연키는 `(case_no, court, decision_date)` 조합. 동일 조합 중복 시 인제스트는 upsert 동작 (본문 업데이트만 수행, ID·embedding은 재사용 가능).

## 임베딩 규칙 (C-4에서 확정)
- 모델: `embed-v4.0`, 1024-dim, `input_type=search_document`
- 입력 텍스트: `"[사건명] " + case_name + "\n[판시사항] " + headnote + "\n[판결요지] " + holding`
- 길이가 Cohere 입력 한계를 넘기면 `reasoning` 앞부분 일부까지 포함하고 이후는 절단.
- `embedding_model` 컬럼에 모델 ID 기록하여 추후 재임베딩 필요성 판단.
