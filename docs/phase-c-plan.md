# Phase C 계획서 — 데이터 커버리지 확장 + 품질 측정 체계

작성: 2026-04-19
선행: Phase B-8 종료 (민법 1,193행 인제스트, 3-way 하이브리드, 관측성, 한국어 BM25 prefix 매칭 완료)
브랜치 전략: `feature/phase-c-*` 시리즈 (B-8 브랜치와 분리하여 병렬 진행 가능)

## 배경

Phase B 종료 시점 RAG 파이프라인은 **기능적으로 완성**됐다. 정성 스모크
(`scripts/rag_qualitative_smoke.py`, 10/10 관련성)에서 기대 조문이 상위
5에 안정적으로 노출되는 것을 확인했다. 그러나 두 가지 본질적 제약이 남아
있다.

1. **데이터 커버리지 제약**: 민법 하나만 수록. 온톨로지에 선언된 특별법
   19개(주택임대차보호법/상가건물임대차보호법 등)와 판례가 부재하여 실제
   법률 상담 범위의 일부만 대응 가능.
2. **정량 측정 부재**: Recall@K, MRR, nDCG를 재현 가능한 형태로 측정하는
   평가 셋이 없다. 가중치 튜닝/재랭커 도입 시 개선 여부를 판단할 기준선이
   없음.

Phase C는 이 두 제약을 정면으로 해소한다.

## 목표

| 지표 | Phase B 종료 | Phase C 목표 |
|---|---|---|
| 인제스트 법령 수 | 1 (민법) | 20 (민법 + 특별법 19) |
| 인제스트 청크 수 | ~1,193 | ~1,600 (특별법 약 400 추가) |
| 판례 인제스트 | 0 | 대표 500~1,000건 (핵심 도메인) |
| 정량 평가 셋 | 없음 | ≥ 30 질의 × 정답 라벨 |
| Recall@5 (eval set) | 미측정 | ≥ 0.85 |
| MRR (eval set) | 미측정 | ≥ 0.65 |
| 재현 가능 벤치 | scripts/verify_3way_search.py (4건) | CI-friendly eval runner |

## 전체 단계 (C-1 ~ C-6)

| 단계 | 제목 | 단위 작업 범위 | 선후 의존 |
|---|---|---|---|
| C-1 | 평가 셋 구축 + 자동 벤치 러너 | 30질의 라벨링, Recall@K/MRR 계산 스크립트, 기준선 리포트 | 없음 — 즉시 착수 가능 |
| C-2 | 특별법 19개 인제스트 파이프라인 | fetch_special_laws.py, 다중 법령 대응 load 스크립트, 재임베딩 | 선행: LAW_OC 키 |
| C-3 | 판례 스키마 + 인제스트 어댑터 | V6/V7 마이그레이션, case_chunks, 판례 소스 어댑터 1종 | 선행: 판례 소스 결정 |
| C-4 | 검색 경로 판례 확장 | LegalRetrievalService 판례 경로, union vs rerank 설계 선택 | C-3 완료 후 |
| C-5 | Cohere Rerank 3.5 도입 | PostRetrievalReranker, outcome 메트릭 태그 확장 | C-1 완료 후 (측정 기준 필요) |
| C-6 | Phase C 종합 보고서 + 가중치 재튜닝 | eval 셋 기반 베스트 파라미터 탐색, 대시보드 JSON 템플릿 | C-1~C-5 완료 후 |

**권장 실행 순서**: C-1 → C-2 → (C-5 또는 C-3) → C-4 → C-6

C-1이 최우선인 이유는 이후 모든 단계의 회귀 방지 기준이 되기 때문이다.
C-5(Rerank)는 데이터와 무관하므로 C-1 직후에도 진행 가능하다.

## C-1. 평가 셋 구축 + 자동 벤치 러너

### 범위
- `eval/eval-set.v1.jsonl` — 질의 30~40개, 각 질의에 정답 청크 ID 또는
  (law_id, article_no) 쌍 라벨링
- `scripts/eval_rag.py` — eval 셋을 로드해 Recall@K, MRR, nDCG, 질의별
  상위 5 테이블을 JSON + Markdown으로 출력
- `docs/phase-c1-baseline.md` — 현 시점(B-8 종료) 기준선 숫자 기록

### 질의 분포 가이드
| 도메인 | 비중 | 예시 |
|---|---|---|
| 부동산 거래/임대차 | 30% | 전세, 임대차, 매매, 소유권 이전 |
| 상속/유류분 | 15% | 한정승인, 유언, 유류분 반환 |
| 채권/보증/시효 | 15% | 대여금, 보증, 소멸시효 |
| 손해배상/불법행위 | 15% | 교통사고, 의료사고, 명예훼손 |
| 가족법 | 10% | 이혼, 재산분할, 양육비 |
| 근로/상사 | 10% | 해고, 임금, 하도급 |
| 희귀 케이스 | 5% | 중의성 있는 키워드 |

### 라벨링 방법
1. 현재 정성 스모크 10건을 출발점으로 확장
2. 각 질의당 정답 후보를 **3개까지** 허용 (민법 조문 단위)
3. 검토 시 `scripts/rag_qualitative_smoke.py` 결과를 참고하되 맹신하지
   않음 — 1위라도 오답이면 정답 라벨에서 제외
4. 애매한 케이스는 `notes` 필드에 판정 사유 기록

### 산출물 스키마
```json
{
  "id": "C1-Q01",
  "query": "전세 계약이 끝났는데 집주인이 보증금을 돌려주지 않음",
  "category_ids": ["group:jeonse"],
  "bm25_keywords": ["전세권", "전세금", "보증금", "반환"],
  "gold_articles": [
    {"law_id": "law-civil", "article_no": "제312조의2"},
    {"law_id": "law-civil", "article_no": "제317조"}
  ],
  "notes": "제303조는 전세권 정의라 제외"
}
```

### 메트릭 정의
- **Recall@K**: 상위 K 결과 중 gold_articles와 교집합이 1개 이상이면 hit.
  K ∈ {1, 3, 5, 10}
- **MRR**: 1 / (첫 번째 gold article 등장 순위). gold 미포함이면 0
- **nDCG@5**: gold article 하나당 relevance=1, 나머지 0의 DCG / IDCG

### 완료 조건
- 30건 이상 라벨링 완료
- `python3 scripts/eval_rag.py --eval eval/eval-set.v1.jsonl` 로
  baseline 숫자 출력
- 커밋 + docs/phase-c1-baseline.md

## C-2. 특별법 19개 인제스트

### 범위
- `scripts/fetch_special_laws.py` — 법제처 OpenAPI로 LSI ID 19개의 본문
  수집, 조문 단위 JSON 시드 생성 (기존 `fetch_civil_law.py` 패턴 확장)
- `scripts/load-legal-chunks.py` 확장 — 다중 law_id 지원, 멱등
  upsert 검증
- 온톨로지 매핑 보완: 특별법 조문 → category_ids 자동 생성 로직
- 재임베딩: 신규 청크만 Cohere embed 호출 (멱등)

### 전제
- **LAW_OC 키 확보 필수**. 없으면 본 단계 진입 불가
- Cohere 무료 trial 한도 확인: 특별법 ~400 chunk × 1024-dim = 약
  400회 embed 호출. 무료 범위 내 추정

### 대상 법령 (온톨로지 LSI 기준)
LSI215759, LSI238797, LSI249997, LSI249999, LSI251943, LSI252747,
LSI259881, LSI265307, LSI265351, LSI265377, LSI267359, LSI267649,
LSI267689, LSI268669, LSI270351, LSI271123, LSI276123, LSI276893, LSI277017

이 중 **주택임대차보호법(LSI249999)**, **상가건물임대차보호법(LSI238797)**은
B-플랜에서 핵심 회귀 대응 조문으로 언급되었으므로 우선 적재한다.

### 검증
- C-1 eval set으로 Recall@5 비교. 특별법 관련 질의(예: 주택임대차
  최우선변제)가 상위에 노출되는지 확인
- 민법 전용 질의의 랭킹이 특별법 추가로 하락하지 않았는지 회귀 확인

### 완료 조건
- 20개 law_id가 DB에 적재
- 재임베딩 로그 + 중복 upsert 시 no-op 확인
- docs/phase-c2-report.md

## C-3. 판례 스키마 + 인제스트 어댑터

### 범위
- V6 마이그레이션: `case_chunks(id, case_number, court, judgment_date,
  case_type, issues, holding, content, content_tsv, embedding,
  category_ids, source_url, ...)`
- V7 마이그레이션: `article_refs(article_id, case_id)` — 조문 ↔ 판례 FK
- `scripts/fetch_cases.py` — 소스 어댑터 1종 (아래 소스 결정 후)
- `scripts/load_cases.py` — chunk 생성 + 임베딩 + 업로드

### 판례 소스 결정 필요
| 후보 | 장점 | 단점 |
|---|---|---|
| 대법원 종합법률정보 API | 공식, 전체 판례 | 키 발급, 호출 제한 |
| 법제처 DRF API | 이미 LAW_OC 재사용 가능 | 판례 커버리지 일부 |
| AI허브 판례 데이터셋 | 즉시 다운로드 | 시점 고정 (2022 등) |

### 초기 범위 제한
전체 판례(수십만 건)를 한 번에 넣지 않는다. **핵심 도메인 500~1,000건**만
샘플링하여 Phase C 범위에서는 파이프라인 검증에 집중.

### 완료 조건
- case_chunks 테이블 생성 + 샘플 500~1,000건 인제스트
- article_refs 연결 (참조조문 파싱이 가능한 경우)
- docs/phase-c3-report.md

## C-4. 검색 경로 판례 확장

### 범위
- `LegalRetrievalService` 인터페이스에 `CaseRetrievalService` 형제 추가
  또는 `retrieve`에 source 파라미터 도입
- RAG 컨텍스트 빌더에 판례 섹션 추가 (조문 섹션과 별도 헤더)
- `shield.rag.retrieve` 메트릭에 `source=article|case` 태그 확장

### 설계 선택 (C-3 결과 보고 결정)
- (A) Union: 조문 검색과 판례 검색 결과를 score 정규화 후 합집합
- (B) Cascade: 조문 검색 → 상위 조문이 참조하는 판례를 article_refs로 join

### 완료 조건
- 판례 포함 질의에 대한 eval 셋 성능 측정 (C-1 재실행)
- docs/phase-c4-report.md

## C-5. Cohere Rerank 3.5 도입

### 범위
- `Rerank` API 어댑터 (`CohereRerankClient`)
- `PostRetrievalReranker` 컴포넌트 — 3-way 상위 K'(예 20) → Rerank →
  상위 K(예 5)
- Feature flag: `rag.rerank.enabled` (기본 false, eval에서 개선 확인 후
  true 전환)
- 메트릭: `shield.rag.rerank` Timer (outcome=success|failure)

### 비용 고려
Cohere Rerank 3.5는 별도 과금. 무료 trial 범위 외가 될 수 있음. 호출
지점을 **3-way 상위 K'만** 보내도록 제한하여 비용 예측 가능하게 함
(예: 질의당 20건 × $0.002).

### 완료 조건
- eval 셋으로 baseline vs rerank 비교표
- docs/phase-c5-report.md

## C-6. 종합 + 가중치 재튜닝 + 대시보드

### 범위
- `scripts/tune_weights.py` — C-1 eval 셋으로 (vector, keyword, trigram)
  가중치 grid search, 베스트 조합 기록
- Grafana 대시보드 JSON 템플릿 (`ops/grafana/rag-dashboard.json`)
- 경보 규칙 코드화 (`ops/prometheus/alerts.yml`)
- docs/phase-c-final-report.md — Phase C 전체 지표 요약 (Recall@5, MRR,
  벤치 시간, Rerank 효과, 특별법 + 판례 커버리지)

## 전제 조건 체크리스트

Phase C 착수 전에 확보되어야 할 것:

- [ ] 법제처 OpenAPI 키 (LAW_OC)
- [ ] Cohere 유료 플랜 또는 embed+rerank trial 한도 확인
- [ ] 판례 소스 결정 (C-3 직전까지 확정)
- [ ] eval set 라벨링 인력 (C-1에서 수작업 필요)

## 리스크

| 리스크 | 영향 | 완화 |
|---|---|---|
| LAW_OC 키 미확보 | C-2 착수 불가 | 운영자 발급 선행 필수 |
| Cohere 비용 초과 | C-2/C-5 중단 | Batch 크기 조절, 무료 범위 사전 추정 |
| 판례 라이선스 | C-3 배포 제약 | 내부 검색용으로 제한, 재배포 금지 주의 |
| eval 셋 과적합 | C-5/C-6 숫자 과장 | hold-out set 20% 분리 |

## 성공 기준

Phase C 종료 시점에 다음 중 최소 4개를 충족한다.

1. 특별법 19개 모두 인제스트 완료
2. 판례 샘플 500건 이상 인제스트 완료
3. eval 셋 Recall@5 ≥ 0.85
4. eval 셋 MRR ≥ 0.65
5. Rerank on/off 비교표 문서화
6. Grafana 대시보드 템플릿 커밋
7. 회귀 방지용 `eval_rag.py`가 CI-friendly 형태로 존재
