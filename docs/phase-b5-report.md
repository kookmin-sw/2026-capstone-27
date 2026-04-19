# Phase B-5 — Redis 쿼리 임베딩 캐시 + HNSW ef_search 튜닝 (완료 보고서)

작성일: 2026-04-19
브랜치: `feature/issue-A-migrate-rag-to-postgres`
선행: B-4 (3-way 하이브리드 검색)

## 목적

Layer 2 RAG 검색의 응답 지연과 Cohere Embed API 호출 비용을 줄이기 위해
(1) 쿼리 임베딩을 Redis에 캐시하고, (2) pgvector HNSW 인덱스의 `ef_search`를
외부화·측정 기반으로 튜닝했다.

## 구현 요약

### 1. 쿼리 임베딩 캐시 아키텍처 (Cache-aside)

```
PgLegalRetrievalService
  └─ QueryEmbeddingService            ← 신규
       ├─ EmbeddingCache.get(model, query)   ← hit이면 return
       └─ miss → CohereClient.embedQuery  → EmbeddingCache.put
```

| 컴포넌트 | 역할 |
|---|---|
| `EmbeddingCache` (인터페이스) | `get` / `put` 추상화 |
| `RedisEmbeddingCache` | `spring.data.redis.host`가 설정된 환경에서 활성화, TTL 24h 기본 |
| `NoopEmbeddingCache` | Redis 미구성 시 기본 주입 (`@ConditionalOnMissingBean`) |
| `QueryEmbeddingService` | Cache-aside 흐름, Cohere fallback 보존 |

**키 설계**: `emb:{model}:{sha256(query.trim())}`

- 한글/특수문자 이스케이핑 회피
- 키 길이 상한 고정 (SHA-256 64 hex)
- 모델 교체 시 자연스러운 무효화

**값 직렬화**: JSON 배열 `"[0.1,-0.2,...]"` (Jackson)

- redis-cli / Redis Insight로 직접 검증 가능
- float32 바이너리 대비 약 3배 크기이지만 1024차원 기준 ≈ 12KB — 캐시 용량상 무시 가능

**장애 허용**: Redis get/put 실패 시 예외를 삼키고 miss 처리 → Cohere 호출 경로로
자연스럽게 흐른다. RAG 파이프라인이 Redis 장애에 영향을 받지 않는다.

### 2. HNSW `ef_search` 튜닝

`PgLegalRetrievalService.retrieve`에서 검색 직전 동일 트랜잭션 내에
`SET LOCAL hnsw.ef_search = N`을 실행한다.

- `@Transactional(readOnly=true)` 범위 내에서만 유효
- 커넥션 풀 반납 시 자동 해제 → 다른 쿼리에 영향 없음
- `rag.retrieval.hnsw.ef-search` 외부화 (`RAG_HNSW_EF_SEARCH` 환경변수)

### 3. 설정 외부화

```yaml
rag:
  retrieval:
    hnsw:
      ef-search: ${RAG_HNSW_EF_SEARCH:40}     # 실측 기반 결정 (아래 벤치)
  cache:
    embedding:
      enabled: ${RAG_EMBED_CACHE_ENABLED:true}
      ttl-seconds: ${RAG_EMBED_CACHE_TTL:86400}  # 24h

spring:
  data:
    redis:
      host: ${REDIS_HOST:}           # 비어 있으면 Noop 캐시 자동 전환
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      client-type: lettuce
```

## HNSW ef_search 벤치마크 결과

스크립트: `scripts/benchmark_hnsw_ef_search.py`
대상: 민법 1,193개 조문, topK=10, 쿼리 4종 × 5회 반복, median 측정

| ef_search | median latency (ms) | avg recall vs ef=400 |
|-----------|---------------------|----------------------|
| 10        | 0.50                | **97.5%** (소유권 케이스에서 90%) |
| **40**    | **0.86**            | **100.0%** ← 기본값 |
| 80        | 11.55               | 100.0% |
| 160       | 11.42               | 100.0% |
| 400       | 11.42               | 100.0% |

**관찰**:

1. 1,193행 규모에서는 pgvector 기본값 `ef=40`으로도 topK=10 recall 100% 달성.
2. 80 이상에서의 10ms 점프는 HNSW 탐색 비용보다 Supabase Pooler의 plan cache 동작
   차이로 추정됨 — 80/160/400 모두 동일 지연.
3. `ef=10`에서만 한 쿼리의 recall이 90%로 떨어졌다 → 하한선.

**결정**: 기본값 `ef_search=40`. 민법 외 판례까지 적재해 후보 수가 수만 건 이상으로
커질 경우 80~200 범위에서 재벤치 후 상향.

## 단위 테스트 (신규)

### `RedisEmbeddingCacheKeyTest` (5건)
- 동일 입력 → 동일 키
- 모델 변경 → 키 분리
- 쿼리 공백 정규화
- 접두어 `emb:` + 64자리 hex SHA-256
- null/blank 모델 → `_` placeholder

### `QueryEmbeddingServiceTest` (4건)
- 캐시 HIT → Cohere 호출 생략
- 캐시 MISS → Cohere 호출 + PUT
- Cohere 실패 → 예외 전파 + 캐시 저장 없음
- 빈 임베딩 응답 → 저장 생략

### `PgLegalRetrievalServiceTest` (7건, B-4 회귀 포함)
- 생성자 변경(7인자 → `QueryEmbeddingService` + `hnswEfSearch`) 반영
- 기존 7건 모두 통과

## 테스트 결과

| 범위 | 결과 |
|---|---|
| B-5 신규 (Redis key + QueryEmbeddingService) | 9/9 pass |
| B-4 회귀 (PgLegalRetrievalServiceTest) | 7/7 pass |
| 전체 | 83/85 pass (2 실패 = pre-existing, B-4 보고서와 동일) |

Pre-existing 실패 2건:
- `ShieldApplicationTests.contextLoads` — PlaceholderResolutionException
- `ChecklistCoverageServiceTest` — 형법 체크리스트 커버리지

B-5 변경으로 인한 신규 회귀 없음을 확인했다.

## 동작 시나리오

### 로컬 개발 (Redis 없음)
1. `REDIS_HOST` 미설정 → `RedisEmbeddingCache` 빈 등록 skip
2. `NoopEmbeddingCache` 주입 → 모든 `get`이 miss, `put`은 no-op
3. `QueryEmbeddingService` → 매 쿼리마다 Cohere 호출
4. HNSW `ef_search=40` 적용 → 0.86ms 평균 지연

### 운영 (Redis 구성)
1. `REDIS_HOST=...` 주입 → `RedisEmbeddingCache` 활성화
2. 동일 쿼리 재요청 시 Cohere 호출 생략 (~100ms 절감)
3. Cohere 429/5xx 발생 시 → 예외 → `PgLegalRetrievalService`에서
   영벡터로 degrade → 2-way(BM25+trigram) 자동 fallback

### Redis 일시 장애
1. `RedisEmbeddingCache.get` 예외 → miss 처리 + 경고 로그
2. Cohere 호출로 폴백
3. `put` 예외도 삼킴 → 사용자 응답 영향 없음

## 파일 변경 요약

### 신규
- `src/main/java/org/example/shield/ai/application/EmbeddingCache.java`
- `src/main/java/org/example/shield/ai/application/QueryEmbeddingService.java`
- `src/main/java/org/example/shield/ai/infrastructure/RedisEmbeddingCache.java`
- `src/main/java/org/example/shield/ai/infrastructure/NoopEmbeddingCache.java`
- `src/test/java/org/example/shield/ai/infrastructure/RedisEmbeddingCacheKeyTest.java`
- `src/test/java/org/example/shield/ai/application/QueryEmbeddingServiceTest.java`
- `scripts/benchmark_hnsw_ef_search.py`
- `docs/phase-b5-report.md`

### 수정
- `build.gradle` — `spring-boot-starter-data-redis` 추가
- `src/main/java/org/example/shield/ai/infrastructure/PgLegalRetrievalService.java`
  — `CohereClient` 의존성 → `QueryEmbeddingService`로 교체, HNSW ef_search 적용 로직 추가
- `src/main/java/org/example/shield/common/config/RedisConfig.java` — 문서화
- `src/main/resources/application.yml` — Redis/cache/HNSW 설정 추가
- `src/test/java/org/example/shield/ai/infrastructure/PgLegalRetrievalServiceTest.java`
  — 새 생성자 시그니처 + `QueryEmbeddingService` mock 반영

## 다음 단계 (B-7)

- `RAG_STUB=false` 운영 전환
- 운영 환경에서 실제 Redis 주입 + 캐시 히트율 모니터링
- 필요 시 Prometheus/Micrometer 메트릭 추가 (B-8 범위)
