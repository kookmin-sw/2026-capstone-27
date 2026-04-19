# Phase C-3 — 판례(legal_cases) 스키마 설계

## 요약

- 목표: 민법(C-1)·특별법(C-2)에 이어 **판례 1건 = 1 row** 단위로 RAG 코퍼스에 편입할 수 있는 스토리지 스키마를 도입. **본 단계에서는 스키마만** 작성하고 실제 시드 인제스트는 C-4에서 진행.
- 산출물: Flyway V6 마이그레이션, `LegalCaseEntity` + `LegalCaseJpaRepository`, 시드 JSON 스펙과 예시, 엔티티 단위 테스트 4건(모두 통과).
- 검증: V6를 Supabase에 적용하여 24 컬럼·10 인덱스·1 유니크 제약·1 트리거 생성 확인. Gradle `compileJava`/`compileTestJava`/신규 테스트 그린.

## 1. 설계 결정 요약

### 1.1 문서 단위 — 판례 1건 = 1 row
법령(`legal_chunks`)은 조문 단위로 분할하지만, 판례는 판시사항·판결요지·판결이유가 유기적으로 연결되어 있어 **통째로 검색되는 편이 자연스럽다**. 실제 대법원 종합법률정보·법제처 판례 OpenAPI도 판례 1건을 한 리소스로 제공한다.

### 1.2 본문 필드 분리
| 컬럼 | 역할 | BM25 포함 | 임베딩 입력 포함 |
|---|---|---|---|
| `case_name` | 사건명 | ○ | ○ |
| `headnote` | 판시사항 (쟁점) | ○ | ○ |
| `holding` | 판결요지 (법리 결론) | ○ | ○ |
| `reasoning` | 판결이유 (판단 근거) | ○ | △ (길면 절단) |
| `full_text` | 원문 전체 | × (노이즈 방지) | × |

- **임베딩 입력 텍스트**(C-4에서 구현): `"[사건명] " + case_name + "\n[판시사항] " + headnote + "\n[판결요지] " + holding`
- Cohere `embed-v4.0` 입력 한계를 넘기면 `reasoning` 앞부분까지 포함 후 절단.

### 1.3 자연키 — `(case_no, court, decision_date)`
한 사건번호(`2020다12345`)에 여러 심급 판결이 나올 수 있어 단일 `case_no`는 자연키로 불충분. 법원·선고일과 결합해 유일성 확보. Flyway V6에서 `UNIQUE INDEX uq_legal_cases_natural_key`로 강제.

### 1.4 임베딩 — `legal_chunks`와 동일
- 모델: Cohere `embed-v4.0`, 1024-dim
- 인덱스: HNSW `vector_cosine_ops`, `m=16`, `ef_construction=64`, `WHERE embedding IS NOT NULL`
- 백필 추적: `idx_legal_cases_embedding_null` 부분 인덱스

### 1.5 BM25(tsvector GENERATED) — `legal_chunks`와 동일 패턴
```sql
content_tsv = to_tsvector('simple',
    case_name || ' ' || case_no || ' ' || headnote || ' ' || holding || ' ' || reasoning)
```
GIN 인덱스로 서비스 레이어에서 3-way 하이브리드 검색에 바로 사용 가능.

### 1.6 pg_trgm — `holding`만
법령은 `content` 전체에 trigram을 걸었지만, 판례는 `holding`(판결요지)에만 걸어 노이즈를 줄인다. 판례 검색의 실무 패턴은 "요지 키워드 → 유사 판례"가 압도적이기 때문.

### 1.7 배열 컬럼 3종 + GIN
- `cited_articles TEXT[]` — 참조 법령·조문. 조문 역조회에 필요. GIN 인덱스로 `cited_articles @> ARRAY['민법 제312조']` 검색 O(log n).
- `cited_cases TEXT[]` — 참조 판례 사건번호.
- `category_ids TEXT[]` — 온톨로지 카테고리. **`legal_chunks.category_ids`와 동일 체계**를 유지하여 카테고리 필터가 법령·판례에 공통 적용.

### 1.8 `full_text` 보존 이유
BM25 인덱스에는 포함시키지 않지만 컬럼 자체는 보존. 향후 RAG 답변 생성 시 컨텍스트 확장용 혹은 법률 전문가용 UI에서 원문 표시용으로 필요할 수 있음.

## 2. 테이블 스키마 (요약 ERD)

```
legal_cases
├── id              BIGSERIAL PK
├── case_no         VARCHAR(64)  NOT NULL  ┐
├── court           VARCHAR(100) NOT NULL  │ UNIQUE
├── decision_date   DATE         NOT NULL  ┘
├── case_name       VARCHAR(500)
├── case_type       VARCHAR(32)  NOT NULL    [민사|형사|가사|행정|특허|헌법|기타]
├── judgment_type   VARCHAR(32)              [판결|결정|명령]
├── disposition     VARCHAR(200)             [상고기각|파기환송|…]
├── headnote        TEXT                     — 판시사항
├── holding         TEXT                     — 판결요지
├── reasoning       TEXT                     — 판결이유
├── full_text       TEXT                     — 원문 전체
├── cited_articles  TEXT[]                   — GIN
├── cited_cases     TEXT[]                   — (인덱스 없음, 저빈도 필터)
├── category_ids    TEXT[]                   — GIN
├── source          VARCHAR(32)  NOT NULL  DEFAULT 'law.go.kr'
├── source_url      VARCHAR(512)
├── source_id       VARCHAR(128)             — 법제처 판례일련번호 등
├── content_tsv     tsvector GENERATED       — GIN
├── embedding       vector(1024)             — HNSW cosine
├── embedding_model VARCHAR(64)
├── created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
└── updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()  — trigger touch_updated_at
```

인덱스 요약 (10개):
- `uq_legal_cases_natural_key` UNIQUE (case_no, court, decision_date)
- `idx_legal_cases_tsv` GIN (content_tsv) — BM25
- `idx_legal_cases_category_ids` GIN (category_ids)
- `idx_legal_cases_cited_articles` GIN (cited_articles)
- `idx_legal_cases_holding_trgm` GIN (holding gin_trgm_ops)
- `idx_legal_cases_embedding_hnsw` HNSW (embedding vector_cosine_ops)
- `idx_legal_cases_embedding_null` partial idx (embedding IS NULL)
- `idx_legal_cases_case_type` btree (case_type)
- `idx_legal_cases_decision_date` btree (decision_date DESC)
- `legal_cases_pkey` (id)

## 3. JPA 매핑

- `LegalCaseEntity` — 22 필드(생성 컬럼 `content_tsv` 제외). 배열·벡터는 `@JdbcTypeCode(SqlTypes.ARRAY)` / `SqlTypes.VECTOR`.
- 업데이터 2종:
  - `updateEmbedding(vec, model)` — 임베딩과 모델 ID 동시 갱신
  - `updateContent(...)` — 재수집 시 본문 필드 반영, embedding은 건드리지 않음 (재임베딩은 별도 경로)
- `LegalCaseJpaRepository` — 자연키/사건번호/유형 필터 조회 + 백필 카운트. 하이브리드 검색 네이티브 쿼리는 C-4 이후 별도 서비스에서 추가.

## 4. 시드 JSON 포맷

파일: `src/main/resources/seed/cases/_SCHEMA.md`(전체 스펙), `_EXAMPLE.json`(예시 1건).

```json
{
  "meta": { "source": "law.go.kr", "source_id": "...", "source_url": "...", "fetched_at": "..." },
  "case": {
    "case_no": "2005다21166",
    "court": "대법원",
    "decision_date": "2005-06-09",
    "case_type": "민사",
    "case_name": "...",
    "headnote": "...",
    "holding": "...",
    "reasoning": "...",
    "cited_articles": ["주택임대차보호법 제3조", ...],
    "cited_cases": [...],
    "category_ids": ["cat-real-estate-lease"]
  }
}
```

C-4 인제스트 파이프라인이 이 포맷을 읽어 `LegalCaseEntity`로 변환 + 임베딩 생성 + upsert.

## 5. 테스트

- `LegalCaseEntityTest` — 4건, 모두 통과
  - 빌더 전체 필드 매핑
  - `source` 기본값 `'law.go.kr'`
  - `updateEmbedding` 두 필드 동시 갱신
  - `updateContent` 실행 시 embedding 비유지

Testcontainers 기반 DB 통합 테스트는 현 레포 컨벤션상 미도입 상태. 대신 본 단계에서는 **V6 DDL을 Supabase DB에 선행 적용**해 스키마 레벨 검증 완료 (24 컬럼·10 인덱스·1 유니크·1 트리거). Flyway 이력은 Spring 부팅 시 자동 등록 예정.

## 6. C-4 인제스트 단계 TODO

1. **판례 수집 스크립트** `scripts/fetch_cases.py`
   - 법제처 판례 OpenAPI (`/DRF/lawSearch.do?target=prec`, `/DRF/lawService.do?target=prec&ID=...`)
   - 또는 온톨로지의 `specialized_laws.csv` 연관 키워드로 초기 200~500건 샘플링
2. **`LegalCaseIngestService` + `LegalCaseIngestRunner`**
   - 민법·특별법과 동일한 배치 업서트 흐름
   - 임베딩 입력 텍스트 조합 로직: `"[사건명] ... \n[판시사항] ... \n[판결요지] ..."`
3. **eval-set v2 확장**
   - C-2 분석(Q01·Q27)에서 확인된 특별법 조문 gold 추가
   - 판례 gold 10건 추가 → `case_no` 단위 hit 측정
4. **검색 통합**
   - `PgLegalRetrievalService`에 `legal_cases` 하이브리드 검색 브랜치 추가
   - Spring 서비스 레벨에서 "법령 후보 + 판례 후보"를 병합하고 rerank 단계로 전달

## 7. 파일 목록

- `src/main/resources/db/migration/V6__create_legal_cases.sql`
- `src/main/java/org/example/shield/ai/domain/LegalCaseEntity.java`
- `src/main/java/org/example/shield/ai/domain/LegalCaseJpaRepository.java`
- `src/main/resources/seed/cases/_SCHEMA.md`
- `src/main/resources/seed/cases/_EXAMPLE.json`
- `src/test/java/org/example/shield/ai/domain/LegalCaseEntityTest.java`
- `docs/phase-c3-schema-design.md` (본 문서)
