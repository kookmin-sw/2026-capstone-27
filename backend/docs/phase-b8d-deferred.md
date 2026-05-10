# Phase B-8d — 판례/특별법 인제스트 확장 (보류)

작성일: 2026-04-19
브랜치: `feature/issue-A-migrate-rag-to-postgres`
상태: **Phase C로 이연**

## 결론

본 단계는 **Phase C로 이연**한다. B-8b(관측성), B-8c(한국어 BM25)는
단일 세션에서 품질/운영 개선 효과를 완결할 수 있었던 반면, B-8d는
데이터 소스 확보 → 스키마 재설계 → 인제스트 파이프라인 확장 → 재임베딩의
선형 체인이 길고, 각 단계가 다른 단계의 전제라 한 번의 세션에서 수행하기에
범위가 지나치게 크다. 또한 아래의 즉시 블로커가 존재한다.

## 보류 사유

### 1. 법제처 OpenAPI 키(LAW_OC) 부재

현재 인제스트 스크립트 `scripts/fetch_civil_law.py`는 `LAW_OC` 환경변수로
국가법령정보 OpenAPI 키를 요구한다. 본 세션 컨텍스트에는 해당 키가 없다.
특별법 본문 수집을 시작할 수 없다.

### 2. 판례 데이터는 별도 스키마가 필요

현재 `legal_chunks` 테이블은 법률 조문을 전제로 설계되어 있다.

```
legal_chunks(id, law_id, law_name, article_no, article_title,
             content, content_tsv, embedding, category_ids,
             effective_date, abolition_date, source_url, chunk_index, ...)
```

판례는 다음과 같은 속성이 추가로 필요하다.

| 필드 | 의미 |
|---|---|
| case_number | 사건번호 (예: 2021다12345) |
| court | 법원 (대법원/서울고등법원/...) |
| judgment_date | 선고일 |
| case_type | 사건유형 (민사/형사/행정/...) |
| parties | 원고/피고 요약 |
| issues | 판시사항 |
| holding | 판결요지 |
| referenced_articles | 참조조문 (기존 law_chunks와 FK 가능) |
| prior_instance | 원심 판결 참조 |

별도 테이블(`case_chunks`)로 분리하고 참조조문 관계(`article_refs`)로
기존 law_chunks와 연결하는 설계가 합리적이다. 즉 3-way 하이브리드 검색도
"조문 vs 판례"를 별도 경로로 다루거나 union 전략을 추가 설계해야 한다.

### 3. 규모와 임베딩 비용

- 특별법 19개(주택임대차보호법/상가임대차보호법 외 17개) × 평균 20조문
  ≈ 400 chunk → Cohere embed-v4.0 무료 trial 범위 내
- 판례: 대법원 종합법률정보 전체가 수십만 건, 최근 5년 대표 판례만
  추린다 해도 수천~수만 건 → 무료 trial 초과, 재임베딩/재랭킹 파이프라인
  전용 설계 필요

## Phase C 진입 시 작업 순서 제안

1. **전제 확인**
   - `LAW_OC` 키 확보
   - Cohere 유료 플랜 승격 또는 대체 임베딩 모델 확정
   - 판례 소스 결정: (a) 대법원 종합법률정보 OpenAPI, (b) 법제처 DRF,
     (c) AI허브 판례 데이터셋 — 라이선스/갱신 주기/커버리지 비교
2. **스키마**
   - `V6__create_case_chunks.sql`: 판례 전용 테이블
   - `V7__create_article_refs.sql`: 조문 ↔ 판례 참조 관계
3. **인제스트 스크립트**
   - `scripts/fetch_special_laws.py`: 온톨로지의 19개 LSI를 순회
   - `scripts/fetch_cases.py`: 판례 소스별 어댑터
   - `scripts/load_cases.py`: chunk 생성 + 임베딩 + 업로드 (기존
     `load-legal-chunks.py` 패턴 재사용)
4. **검색 경로 확장**
   - `LegalRetrievalService`에 판례 경로 추가 (union vs rerank 중 선택)
   - 또는 별도 `CaseRetrievalService` 인터페이스 분리
5. **관측**
   - `shield.rag.retrieve`에 `source=article|case` 태그 확장

## B-8 정리

| 항목 | 상태 | 비고 |
|---|---|---|
| B-8a | 완료 | categoryIds soft-filter 활성화 (504c087) |
| B-8b | 완료 | Prometheus/Micrometer RAG 계측 (bc2e4dc) |
| B-8c | 완료 | 한국어 BM25 prefix 매칭 + trigram GIN (13034bc) |
| B-8d | **이연** | 본 문서 |

B-8의 핵심 품질/관측성 개선 항목(8a/8b/8c)이 완료되어 운영 파이프라인은
측정 가능하고 한국어 리콜이 보강된 상태다. B-8d는 데이터 커버리지 확장에
해당하며 Phase C의 데이터 플래닝 단계에서 다룬다.
