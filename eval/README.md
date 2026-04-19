# RAG 평가 셋

Phase C-1에서 도입된 정량 평가 기반 자료.

## 파일

- `eval-set.v1.jsonl` — 버전 1 평가 셋. JSONL (한 줄에 한 질의)
- `schema.json` — JSON Schema (기계 검증용)

## 스키마

각 항목은 다음 필드를 가진다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id` | string | O | `C1-Q##` 형식의 고유 ID |
| `domain` | string | O | 도메인 태그 (real_estate/family/inheritance 등) |
| `query` | string | O | 사용자 질의 원문 (Cohere 벡터 임베딩 입력) |
| `category_ids` | string[] \| null | O | soft-filter용 `legal_chunks.category_ids` 값 |
| `bm25_keywords` | string[] | O | BM25 키워드 후보 (prefix 매칭 적용) |
| `gold_articles` | object[] | O | 정답 조문 리스트. 각 요소: `{law_id, article_no}` |
| `notes` | string | X | 라벨링 판정 사유 / 주의사항 |

## 라벨링 원칙

1. **gold_articles 최대 3개**. 조문이 딱 하나만 명확하면 1개만 라벨.
2. 민법 전 영역(총칙/물권/채권/친족/상속)에서 고루 선정.
3. 상위 5에 gold 조문이 하나라도 들어오면 Recall@5 hit.
4. DB에 실제 존재하는 조문만 정답으로 사용 (V1 기준 `law-civil`만 가능).

## 재생성

```bash
COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/eval_rag.py \
    --eval eval/eval-set.v1.jsonl --output docs/phase-c1-baseline.md
```

## 버전 이력

- v1 (2026-04-19): 초기 30 질의. 민법 범위. Phase B-8 종료 기준선.
