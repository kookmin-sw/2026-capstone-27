# Phase B-8b — RAG 관측성 (Micrometer + Prometheus) 완료 보고서

작성일: 2026-04-19
브랜치: `feature/issue-A-migrate-rag-to-postgres`
선행: B-5 (쿼리 임베딩 Redis 캐시), B-7 (PG 경로 운영 전환), B-8a (categoryIds 활성화)

## 목적

B-7에서 RAG 파이프라인을 운영(`RAG_STUB=false`)으로 전환하면서 PgLegalRetrievalService,
Cohere embed, 임베딩 캐시가 모두 크리티컬 경로에 들어갔다. 그러나 각 구성요소의
건강성을 외부에서 관측할 수단이 없어 장애 발생 시 로그 외에는 단서가 없었다.
본 단계에서는 Spring Boot Actuator + Micrometer(Prometheus registry)를 통해 RAG
핵심 지표를 /actuator/prometheus 엔드포인트로 노출한다.

## 계측 카탈로그

| 메트릭 이름 | 타입 | 태그 | 의미 | 호출 지점 |
|---|---|---|---|---|
| `shield.rag.embedding.cache` | Counter | `result=hit\|miss` | 쿼리 임베딩 캐시 적중/미적중 건수 | `QueryEmbeddingService.embedQuery` |
| `shield.rag.cohere.embed` | Timer | `outcome=success\|failure` | Cohere `/v2/embed` 호출 지연 분포 | `QueryEmbeddingService.embedQuery` (`timeCohereEmbed` 래핑) |
| `shield.rag.retrieve` | Timer | `outcome=success\|failure\|empty` | 3-way retrieve 전체 수행 시간. 0건 리턴은 `empty`로 분리 | `PgLegalRetrievalService.retrieve` |
| `shield.rag.vector.degrade` | Counter | `reason=empty_query\|empty_response\|cohere_error` | 벡터 경로가 영벡터로 degrade된 횟수 | `PgLegalRetrievalService.buildQueryVectorLiteral` |
| `shield.rag.pipeline.fallback` | Counter | — | MessageService가 RAG 예외로 RAG-less fallback한 횟수 | `MessageService.sendMessage` catch |

모든 메트릭에는 Actuator 공통 태그로 `application=shield-backend`
(`spring.application.name`)가 자동 부여된다.

## 설계 결정

### 1. 중앙 센서 (`RagMetrics`)

메트릭 이름과 태그 컨벤션을 한 곳에서 통제하기 위해 `RagMetrics`
(`org.example.shield.ai.infrastructure.RagMetrics`)를 단일 진입점으로 둔다.
MeterRegistry는 Spring Boot가 자동 제공하는 `PrometheusMeterRegistry`가 주입된다.

호출부는 다음과 같이 단순 API만 사용한다.

```java
ragMetrics.recordCacheHit();
ragMetrics.recordCacheMiss();
T result = ragMetrics.timeCohereEmbed(() -> cohereClient.embedQuery(...));
Timer.Sample sample = ragMetrics.startRetrieve();
ragMetrics.stopRetrieveSuccess(sample, hits); // hits==0이면 outcome=empty로 자동 분류
ragMetrics.stopRetrieveFailure(sample);
ragMetrics.recordVectorDegrade("cohere_error");
ragMetrics.recordPipelineFallback();
```

### 2. outcome=empty 분리

"retrieve가 성공했지만 0건" 상황은 대개 categoryIds 필터가 과하게 좁거나
DB 커버리지가 부족한 경우를 의미한다. 단순 success로 집계하면 지표상 건강해
보이지만 실제로는 사용자 경험에 직결되는 품질 저하이므로 별도 라벨로 분리했다.

### 3. vector.degrade reason 라벨

벡터 경로 fallback 원인을 3가지로 분리하여 대응 우선순위를 구분한다.

- `empty_query`: 호출부 버그. 상위에서 방어되어야 함
- `empty_response`: Cohere가 200 OK로 응답하면서 vec이 비어있는 경우. API 이상 징후
- `cohere_error`: HTTP 오류. 가장 흔하며 재시도/써킷 브레이커 대상

### 4. Security 예외

Spring Security가 모든 엔드포인트를 인증으로 막기 때문에
`/actuator/health`, `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`를
`permitAll` 목록에 추가했다. 운영 환경에서는 reverse-proxy/IP 화이트리스트로
보호하는 것을 전제로 한다(K8s 환경이면 NetworkPolicy로 Prometheus Pod만 허용).

## 파일 변경

- `build.gradle` — `spring-boot-starter-actuator`, `io.micrometer:micrometer-registry-prometheus` 추가
- `src/main/resources/application.yml` — `management.endpoints.web.exposure.include`에 `health,info,prometheus`, `management.metrics.tags.application=${spring.application.name}` 추가
- `src/main/java/org/example/shield/common/config/SecurityConfig.java` — actuator permitAll 경로 추가
- `src/main/java/org/example/shield/ai/infrastructure/RagMetrics.java` — NEW. 중앙 센서
- `src/main/java/org/example/shield/ai/application/QueryEmbeddingService.java` — cache hit/miss + Cohere timer 주입
- `src/main/java/org/example/shield/ai/infrastructure/PgLegalRetrievalService.java` — `retrieve` 외부 wrapper + `doRetrieve` 내부 분리, `buildQueryVectorLiteral`에 degrade 카운터 3종
- `src/main/java/org/example/shield/consultation/application/MessageService.java` — RAG catch에 pipeline.fallback 카운터
- `src/test/java/org/example/shield/ai/application/QueryEmbeddingServiceTest.java` — `SimpleMeterRegistry` 기반 메트릭 검증 테스트 3건 추가
- `src/test/java/org/example/shield/ai/infrastructure/PgLegalRetrievalServiceTest.java` — 생성자 시그니처에 `RagMetrics` 반영 (기존 테스트 모두 유지)

## 테스트 결과

`./gradlew test` 기준 **91 passed / 2 failed**. 실패 2건은 Phase B-4부터 지속된
pre-existing 이슈로 B-8b와 무관하다.

- `ShieldApplicationTests.contextLoads` — `DB_URL` placeholder 미해결
- `ChecklistCoverageServiceTest[형법]` — 형법 체크리스트 커버리지 기대치 불일치

추가된 메트릭 테스트(모두 PASS):

- `QueryEmbeddingServiceTest.metrics_cacheHit_증가`
- `QueryEmbeddingServiceTest.metrics_cacheMiss_및_Cohere_timer_증가`
- `QueryEmbeddingServiceTest.metrics_Cohere_failure_시_failure_timer_기록`

## 운영 가이드

### 로컬 확인

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
# 다른 터미널
curl -s http://localhost:8080/actuator/prometheus | grep shield_rag
```

예상 출력 샘플:

```
shield_rag_embedding_cache_total{application="shield-backend",result="hit"} 0.0
shield_rag_embedding_cache_total{application="shield-backend",result="miss"} 0.0
shield_rag_cohere_embed_seconds_count{application="shield-backend",outcome="success"} 0.0
shield_rag_retrieve_seconds_count{application="shield-backend",outcome="success"} 0.0
```

### Prometheus scrape 예시

```yaml
scrape_configs:
  - job_name: shield-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["shield-backend:8080"]
```

### 추천 경보 규칙 (초안)

```
# 벡터 경로가 5분간 20% 이상 degrade
sum(rate(shield_rag_vector_degrade_total[5m])) /
sum(rate(shield_rag_retrieve_seconds_count[5m])) > 0.2

# RAG 파이프라인 fallback 비율 상승
rate(shield_rag_pipeline_fallback_total[5m]) > 0.1

# Cohere p95 지연 급증
histogram_quantile(0.95, sum(rate(shield_rag_cohere_embed_seconds_bucket[5m])) by (le)) > 2
```

## 후속 작업

- Grafana 대시보드 JSON 템플릿 추가 (Phase C 운영 단계)
- Cohere 호출에 Resilience4j retry + circuit breaker 적용 (degrade 카운터 분석 후)
- HikariCP 커넥션 풀 메트릭(`hikaricp_*`)은 Actuator 기본으로 노출됨 — 대시보드에만 반영
