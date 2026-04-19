# Phase B-7 — RAG_STUB=false 운영 전환 (완료 보고서)

작성일: 2026-04-19
브랜치: `feature/issue-A-migrate-rag-to-postgres`
선행: B-1/B-2 (스키마+인제스트), B-4 (3-way 하이브리드), B-5 (Redis 캐시 + HNSW 튜닝)

## 목적

RAG Layer 2 검색을 Stub에서 PostgreSQL 기반 실구현(`PgLegalRetrievalService`)으로
운영 전환한다. 개발/테스트 목적의 Stub 경로는 opt-in 환경변수로 유지한다.

## 변경 요약

### application.yml 기본값 반전

```yaml
rag:
  retrieval:
    stub: ${RAG_STUB:false}   # 이전: true → 현재: false
```

앱 기동 시 기본으로 `PgLegalRetrievalService`가 활성화되어 Supabase pgvector
기반 3-way 하이브리드 검색을 수행한다.

### 조건 분기 (기존 구조 유지)

| 빈 | `@ConditionalOnProperty` | 현재 기본 상태 |
|---|---|---|
| `StubLegalRetrievalService` | `havingValue=true, matchIfMissing=false` | 비활성 |
| `PgLegalRetrievalService` | `havingValue=false, matchIfMissing=false` | 활성 |

두 빈은 mutually exclusive하며 yml 기본값이 주입되므로 property는 항상 존재한다.
`matchIfMissing`은 의도 명확화 차원에서 둘 다 `false`로 둔다.

### Stub 경로 처리 정책

**유지** (opt-in). 삭제하지 않는 이유:

- Cohere API 키가 없는 로컬 개발 환경에서 RAG 파이프라인을 돌려볼 수 있다
- `MessageService`의 RAG-less fallback 경로(ragContext 빈 문자열)를 결정적으로 검증 가능
- 삭제 비용 대비 이득이 작다

`RAG_STUB=true` 환경변수로 언제든 복귀할 수 있다.

## 호출부 영향

`MessageService.processMessage`는 4-인자 레거시 `retrieve(vectorQuery, keywords, lawIds, topK)`를
호출한다. B-4에서 이 시그니처는 5-인자 본체에 `categoryIds=null`로 default-위임하도록
유지되었으므로 호출부 변경 없이 바로 전환된다.

다만 B-4에서 추가한 **category 필터**가 현재 호출부에서 활용되지 않고 있다.
의도 분류 결과의 `matchedNodeIds`를 `categoryIds`로 전달하면 검색 정확도가
추가로 개선될 여지가 있다 — 이는 B-8 범위로 분리한다.

## 운영 경로 요약

정상 경로:

```
MessageService
  → PgLegalRetrievalService.retrieve (rag.retrieval.stub=false)
    → QueryEmbeddingService.embedQuery  (B-5)
         ├─ EmbeddingCache.get         (Redis 또는 Noop)
         └─ Cohere Embed v2 (miss 시)
    → legal_chunks search3Way 네이티브 쿼리 (B-4)
         └─ SET LOCAL hnsw.ef_search=40 (B-5)
    → top-K LegalChunk 반환
```

장애 시 fallback:

- Cohere 임베딩 실패 → 영벡터 degrade → 2-way(BM25+trigram)로 자동 축소
- Redis 일시 장애 → miss 처리, Cohere 호출 경로 유지
- PG 쿼리 예외 → `MessageService`의 try/catch가 ragContext="" 로 fallback,
  RAG-less 응답으로 계속 진행

## 테스트

| 범위 | 결과 |
|---|---|
| 전체 | 83/85 pass |
| B-4 회귀 (PgLegalRetrievalServiceTest) | 7/7 pass |
| B-5 회귀 (RedisEmbeddingCacheKey, QueryEmbeddingService) | 9/9 pass |
| B-7 신규 회귀 | 없음 |

Pre-existing 실패 2건 (B-4 보고서부터 기록):

- `ShieldApplicationTests.contextLoads` — `DB_URL` placeholder 미해결 (테스트용 env 미설정)
- `ChecklistCoverageServiceTest` — 형법 체크리스트 커버리지

두 건 모두 B-7과 무관하며, `TEST-ShieldApplicationTests.xml`에서 실패 원인이
`Could not resolve placeholder 'DB_URL'`로 B-4와 동일함을 확인했다.

## 배포 가이드

### 운영 환경 (전환 후)

기본 기동으로 PG 경로가 활성화되므로 추가 환경변수 불필요. 필요시만 튜닝:

```
RAG_HNSW_EF_SEARCH=40      # 기본 40 (1,193행 recall 100%)
RAG_W_VECTOR=0.5           # 3-way 가중치 (기본)
RAG_W_KEYWORD=0.3
RAG_W_TRIGRAM=0.2
RAG_EMBED_CACHE_ENABLED=true  # Redis 캐시 on/off
RAG_EMBED_CACHE_TTL=86400     # 24h
REDIS_HOST=...                # 설정 시 RedisEmbeddingCache 활성, 없으면 Noop
```

### 로컬 개발 (Cohere 키 없이)

```
RAG_STUB=true
```

Stub이 등록되어 빈 청크를 반환하고, MessageService는 RAG-less 경로로 진행한다.

### 롤백

문제 발생 시:

```
RAG_STUB=true
```

한 줄 환경변수로 즉시 Stub 경로 복귀. 앱 재기동만으로 롤백 완료.

## 파일 변경

- `src/main/resources/application.yml` — `rag.retrieval.stub` 기본값 `true → false`, 주석 재정리
- `src/main/java/org/example/shield/ai/infrastructure/StubLegalRetrievalService.java`
  — Javadoc 재작성, `matchIfMissing=false` (의도 명확화)
- `docs/phase-b7-report.md` — 본 문서

## Phase B 최종 상태

| Step | 상태 |
|---|---|
| B-6 R-17 재검증 | 완료 |
| B-1 V4 스키마 | 완료 |
| B-2 인제스트 | 완료 (1,193행) |
| B-4 3-way 하이브리드 | 완료 |
| B-5 Redis 캐시 + HNSW 튜닝 | 완료 |
| **B-7 운영 전환** | **완료 (본 PR)** |
| B-8 잔존 TODO | Optional / 보류 |

## B-8 후보 (참고)

운영 전환 후 여유가 있을 때 고려할 항목:

- `MessageService`에서 `categoryIds` 전달 활성화 (의도 분류기 → retrieval)
- 한국어 BM25 개선: `pg_bigm` 또는 커스텀 regconfig 평가
- Prometheus/Micrometer 메트릭 (캐시 히트율, HNSW 지연, degrade 빈도)
- 판례/특별법 인제스트로 `legal_chunks` 확장 → ef_search 재튜닝
