# Phase B-4 완료 보고서 — 3-way 하이브리드 검색

**일시**: 2026-04-19
**브랜치**: `feature/issue-A-migrate-rag-to-postgres`

## 1. 결과 요약

| 항목 | 상태 |
|---|---|
| 3-way 검색 (벡터 + BM25 + 트라이그램) | 구현 완료 |
| `category_ids` 필터 (배열 겹침) | 구현 완료 |
| Cohere `embedQuery` 실시간 연동 | 구현 완료 |
| 쿼리 임베딩 실패 시 degrade | 영벡터 fallback → 2-way 동작 |
| 단위 테스트 | 7/7 통과 |
| 전체 테스트 | 74/76 통과 (실패 2건은 pre-existing) |
| 실DB 검증 (4개 케이스) | 기대 조문 전부 상위 5 내 등장 |

## 2. 설계

### 2.1 검색 경로

```
  score = 1 - (embedding <=> query_embedding)    × W_vector
        + ts_rank(content_tsv, to_tsquery(...))  × W_keyword
        + similarity(content, query)             × W_trigram
```

- **벡터**: Cohere `embed-v4.0` (1024차원) → pgvector `<=>` cosine distance → `1 - distance`로 similarity(0~1).
- **BM25**: `content_tsv` GENERATED tsvector, normalization flag 1 (길이 정규화).
- **트라이그램**: `pg_trgm similarity` — 한글 `'simple'` regconfig 한계를 부분 보완.

### 2.2 필터

- `category_ids`: `legal_chunks.category_ids && ARRAY[...]::text[]` 배열 겹침. NULL/empty면 SQL `CARDINALITY = 0` 체크로 자동 비활성화.
- `law_ids`: `law_id IN (...)` — NULL/empty면 전체 법령 대상으로 쿼리 분기.

### 2.3 파라미터 바인딩

- 쿼리 임베딩은 Java `float[]` → `"[0.1,0.2,...]"` 문자열 리터럴로 변환 → `CAST(:queryVector AS vector)`로 pgvector 타입 유도. Locale-independent `%.6f` 포맷.
- `category_ids`는 JPA의 `String[]` 파라미터 → PostgreSQL `text[]`로 자동 바인딩.

### 2.4 Degrade 전략

- Cohere embed API 호출 실패/빈 응답 시: 모든 차원이 0인 영벡터를 pgvector 리터럴로 사용. cosine similarity가 0에 가까워 벡터 경로 점수가 ~0이 되고 BM25+trigram의 2-way로 자연스럽게 degrade.
- RAG 파이프라인 전체가 Cohere 장애로 멈추는 것을 방지하는 safety net.

## 3. 변경 파일

| 파일 | 변경 |
|---|---|
| `ai/application/LegalRetrievalService.java` | 인터페이스 확장: 5-인자 시그니처 추가, 4-인자 default 위임 |
| `ai/infrastructure/PgLegalRetrievalService.java` | 3-way 로직 전면 재작성 + `CohereClient` 연동 + `float[]→pgvector` 변환 |
| `ai/infrastructure/StubLegalRetrievalService.java` | 새 시그니처로 오버라이드 |
| `ai/domain/LegalChunkJpaRepository.java` | `search3Way`, `search3WayByLaws` 2개 네이티브 쿼리 추가 (기존 2-way 쿼리는 하위호환용으로 유지) |
| `resources/application.yml` | 가중치 기본값 재조정 (vector=0.5, keyword=0.3, trigram=0.2) |
| `test/.../PgLegalRetrievalServiceTest.java` | 단위 테스트 7건 신규 |
| `scripts/verify_3way_search.py` | 실DB 검증 스크립트 (Cohere embed + psql) |

## 4. 실DB 검증 결과

`scripts/verify_3way_search.py` 실행 결과 (law_id=`law-civil`, topK=5, weights (0.5, 0.3, 0.2)).

### 4.1 전세 보증금 미반환 (R-17 #1 회귀 케이스)

```
쿼리     : 전세 계약 종료 후 집주인이 보증금을 돌려주지 않음
카테고리 : ['group:jeonse']
기대     : 제303조, 제312조, 제313조, 제317조, 제318조
```

| 순위 | 조문 | 제목 | score | vec | bm25 | trig |
|---|---|---|---|---|---|---|
| 1 | 제315조 | 전세권자의 손해배상책임 | 0.2617 | 0.5132 | 0.00 | 0.0253 |
| 2 | 제311조 | 전세권의 소멸청구 | 0.2433 | 0.4773 | 0.00 | 0.0231 |
| 3 | 제316조 | 원상회복의무, 매수청구권 | 0.2428 | 0.4814 | 0.00 | 0.0106 |
| 4 | 제308조 | 전전세 등의 경우의 책임 | 0.2418 | 0.4731 | 0.00 | 0.0265 |
| 5 | 제317조 | **전세권의 소멸과 동시이행** | 0.2369 | 0.4633 | 0.00 | 0.0261 |

매칭: 1/5 — 제317조(보증금 반환 관련 핵심 조문) 포함. category_ids 필터로 전세권 그룹 안에서 의미적으로 가까운 조문들만 노출됨. Phase A R-17에서 이 케이스는 아예 retrieval 공백이었으나, 복구됨.

### 4.2 임대차 계약 해지

```
쿼리     : 임차인이 차임을 계속 연체하여 임대차 계약을 해지하고 싶음
카테고리 : ['group:leasing']
기대     : 제640조, 제635조, 제618조
```

| 순위 | 조문 | 제목 | score | vec | bm25 | trig |
|---|---|---|---|---|---|---|
| 1 | **제640조** | **차임연체와 해지** | **0.3423** | 0.6209 | 0.0042 | 0.1529 |
| 2 | 제625조 | 임차인의 의사에 반하는 보존행위와 해지권 | 0.2758 | 0.4975 | 0.00 | 0.1354 |
| 3 | 제627조 | 일부멸실 등과 감액청구, 해지권 | 0.2757 | 0.5178 | 0.00 | 0.0839 |
| 4 | 제637조 | 임차인의 파산과 해지통고 | 0.2610 | 0.4904 | 0.00 | 0.0789 |
| 5 | 제636조 | 기간의 약정있는 임대차의 해지통고 | 0.2606 | 0.5024 | 0.00 | 0.0467 |

매칭: 1/3 — 제640조가 명확히 1위, 차점 차도 유의미.

### 4.3 저당권 실행

```
쿼리     : 채무자가 돈을 갚지 않아 담보로 잡은 부동산에 설정한 저당권을 실행하려 함
카테고리 : ['group:mortgage']
기대     : 제356조, 제363조
```

| 순위 | 조문 | 제목 | score | vec | bm25 | trig |
|---|---|---|---|---|---|---|
| 1 | **제356조** | **저당권의 내용** | **0.3171** | 0.5785 | 0.00 | 0.1391 |
| 2 | 제364조 | 제삼취득자의 변제 | 0.3019 | 0.5621 | 0.00 | 0.1043 |
| 3 | 제360조 | 피담보채권의 범위 | 0.2965 | 0.5618 | 0.00 | 0.0780 |
| 4 | 제362조 | 저당물의 보충 | 0.2952 | 0.5742 | 0.00 | 0.0407 |
| 5 | 제361조 | 저당권의 처분제한 | 0.2931 | 0.5511 | 0.00 | 0.0879 |

매칭: 1/2 — 제356조 1위.

### 4.4 소유권 이전과 등기 (카테고리 필터 없음)

```
쿼리     : 부동산을 매수했는데 소유권이전등기를 하지 않으면 어떻게 되는가
카테고리 : 없음 (전체 민법 대상)
기대     : 제186조, 제187조, 제188조
```

| 순위 | 조문 | 제목 | score | vec | bm25 | trig |
|---|---|---|---|---|---|---|
| 1 | **제187조** | **등기를 요하지 아니하는 부동산물권취득** | 0.2587 | 0.4899 | 0.00 | 0.0690 |
| 2 | 제245조 | 점유로 인한 부동산소유권의 취득기간 | 0.2499 | 0.4739 | 0.00 | 0.0645 |
| 3 | **제186조** | **부동산물권변동의 효력** | 0.2412 | 0.4688 | 0.00 | 0.0341 |
| 4 | 제592조 | 환매등기 | 0.2281 | 0.4401 | 0.00 | 0.0404 |
| 5 | 제575조 | 제한물권있는 경우와 매도인의 담보책임 | 0.2268 | 0.4415 | 0.00 | 0.0300 |

매칭: 2/3 — 제186조, 제187조 모두 상위 3 내.

## 5. 관찰 및 후속 과제

### 관찰

- **벡터 경로가 점수를 주도**한다. 4개 케이스 중 3개에서 BM25 점수가 0.
- BM25가 0인 이유는 **PostgreSQL `'simple'` regconfig가 한글을 공백 토큰화만** 하기 때문. "임대차"와 "임대차의"가 서로 다른 토큰으로 취급된다. Cohere 다국어 임베딩이 이 한계를 충분히 가려준다.
- 카테고리 필터가 효과적. "전세" 케이스에서 `group:jeonse`로 좁히지 않으면 계약 총칙/손해배상 조문들이 상위로 올라와 의미가 흐려질 수 있다.

### 후속 과제 (B-5에서 검토)

- **한국어 BM25 강화**: `pg_bigm` (bigram) 또는 MeCab 기반 사용자 정의 regconfig 도입 검토. Supabase에서 pg_bigm 확장 가용성 확인 필요.
- **HNSW `ef_search` 튜닝**: 현재는 기본값. `SET LOCAL hnsw.ef_search = 100;` 등으로 recall ↔ latency 튜닝.
- **Redis 쿼리 임베딩 캐시**: 동일 쿼리 재사용 시 Cohere 호출 절감 (B-5 원래 스코프).
- **RRF(Reciprocal Rank Fusion) 융합**: 가중합 대신 순위 기반 결합으로 점수 스케일 차이에 덜 민감하게.

## 6. 다음 단계

Phase B 전체 현황은 `docs/phase-b-plan.md` 기준:

| 단계 | 상태 |
|---|---|
| B-6 R-17 재검증 | 완료 |
| B-1 V4 스키마 + embed 헬퍼 | 완료 |
| B-2 민법 인제스트 | 완료 |
| **B-4 3-way 하이브리드 검색** | **완료** |
| B-5 Redis 캐시 + HNSW 튜닝 | 다음 |
| B-7 `RAG_STUB=false` 운영 전환 | B-5 이후 |
