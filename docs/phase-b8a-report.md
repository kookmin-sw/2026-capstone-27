# Phase B-8a — categoryIds 전달 활성화 (완료 보고서)

작성일: 2026-04-19
브랜치: `feature/issue-A-migrate-rag-to-postgres`
선행: B-4 (3-way 하이브리드 + categoryIds 파라미터 도입), B-7 (PG 경로 운영 전환)

## 목적

B-4에서 `PgLegalRetrievalService.retrieve`에 도입한 `categoryIds` soft-filter가
지금까지 호출부에서 활용되지 않고 있었다. Layer 1 의도 분류 결과
(`IntentClassificationResult.matchedNodeIds`)를 DB의 `legal_chunks.category_ids`
네임스페이스로 매핑하여 3-way 검색의 후보군을 우선 강화한다.

## 문제 배경

두 네임스페이스가 상이함을 확인했다.

| 소스 | 예시 값 | 네임스페이스 |
|---|---|---|
| 온톨로지 (`IntentClassificationResult.matchedNodeIds`) | `law-001-02`, `law-001-02-02` | 슬림 온톨로지 L2/L3 노드 |
| DB (`legal_chunks.category_ids`) | `book:제3편 채권`, `chapter:제2장 계약`, `group:jeonse` | 법전 구조 + 의미 그룹 |

매핑 없이 `matchedNodeIds`를 그대로 `categoryIds`로 전달하면 DB에 동일한 값이
존재하지 않아 모든 필터 결과가 빈으로 나온다. 따라서 **명시적 매핑 테이블**이
선행 조건이다.

### DB의 category_ids 분포 (1,193행 기준)

| 접두사 | 고유값 수 (대략) | 총 매칭 수 |
|---|---|---|
| `book:` | 5 | 1,193 |
| `chapter:` | 수십 | 1,193 |
| `section:` | 수십 | 1,017 |
| `group:` | **7** | **232** |

`group:` 네임스페이스는 민법 내 의미 묶음으로 한정되어 있어(`jeonse`,
`leasing`, `mortgage`, `pledge`, `ownership`, `claim_effect`,
`guaranty_debtors`) 가장 정밀한 soft-filter 키로 활용 가능하다.

## 설계

### YAML 스키마 확장

`category-law-mappings.yml`의 각 L2 매핑에 선택적 `category_ids` 필드 추가.
**민법 `group:` 7종에 한해** 의미적으로 대응하는 L2 노드에 매핑을 걸었다.
민법 밖 노드(근로·세무·형사 등)는 미등록 — 현 DB에는 대응할 `group:` 값이
없기 때문.

```yaml
law-001-02:  # 부동산 임대차
  primary:   [{ law_id: "LSI249999", name: "주택임대차보호법" }, ...]
  secondary: [...]
  category_ids: ["group:leasing", "group:jeonse"]   # 신규

law-001-03:  # 부동산 담보
  ...
  category_ids: ["group:mortgage", "group:pledge"]

law-006-01:  # 금전채권 및 채무
  ...
  category_ids: ["group:claim_effect"]

law-006-02:  # 보증
  ...
  category_ids: ["group:guaranty_debtors"]

law-007-01:  # 주택임대차보호
  ...
  category_ids: ["group:leasing", "group:jeonse"]
```

전체 7개 L2 노드에 매핑 등록.

### 도메인 / 서비스 변경

`CategoryLawMapping` POJO에 `List<String> categoryIds` 필드 추가.

`CategoryLawMappingService`에 신규 메서드:

```java
public List<String> resolveCategoryIds(List<String> categoryNodeIds)
```

- null/empty 입력 → `List.of()`
- L3 노드 요청 시 하이픈 3개 이상이면 L2 부모로 폴백(기존 `resolveLawIds` 규칙 재사용)
- 매핑이 없는 노드는 조용히 건너뛰어 빈 리스트 반환
- `LinkedHashSet`으로 다중 노드 토큰 중복 제거

### 호출부 (MessageService)

Layer 2 진입 직전 3줄 추가:

```java
List<String> categoryIds = categoryLawMappingService.resolveCategoryIds(
        classification.matchedNodeIds());
List<LegalChunk> chunks = legalRetrievalService.retrieve(
        vectorQuery,
        classification.keywords().core(),
        categoryIds,   // B-8a 신규
        lawIds,
        3);
```

4-인자 레거시 retrieve → 5-인자 정식 retrieve로 호출 전환. B-4에서 이미
5-인자 시그니처가 본체이고 4-인자는 `categoryIds=null`을 위임하는 구조였으므로
호환성 이슈 없음.

## 동작 방식 (soft-filter)

`PgLegalRetrievalService`는 `category_ids && ARRAY[...]` 배열 겹침 조건을
**하드 필터가 아닌 가중치 부여**로 쿼리에 합성한다 (B-4 설계). 즉:

- 매핑이 있는 의도(예: 임대차 → `group:leasing`, `group:jeonse`):
  해당 55개 청크가 우선 후보로 상위 점수에 편입되지만, 점수가 높은 다른 청크도
  top-K에 진입 가능.
- 매핑이 없는 의도(예: 이혼·근로·상속): `categoryIds=[]`로 기존 B-7 동작과
  **동일**. 벡터/BM25/트라이그램 점수만으로 순위.

따라서 B-8a는 **기존 경로에 대한 파괴적 변경 없이 임대차/담보/채권 도메인에만
정밀도를 추가**하는 변경이다.

## 테스트

### 신규 단위 테스트 (CategoryLawMappingServiceTest)

| 테스트 | 결과 |
|---|---|
| `resolveCategoryIds_l2Mapped` | L2 노드 → `[group:leasing, group:jeonse]` 검증 |
| `resolveCategoryIds_l3FallbackToL2` | L3 노드 `law-001-02-02` → L2 부모로 폴백 |
| `resolveCategoryIds_noMappingReturnsEmpty` | 미매핑 노드(`law-004-01`)는 빈 리스트 |
| `resolveCategoryIds_dedup` | 다중 노드 매핑 중복 제거 |
| `resolveCategoryIds_nullSafe` | null/empty 입력 안전 |

CategoryLawMappingServiceTest **11/11 pass** (기존 6 + 신규 5).

### 전체 테스트

| 범위 | 결과 |
|---|---|
| 전체 | 88/90 pass |
| B-8a 신규 | 5/5 pass |
| B-4/B-5 회귀 | 이상 없음 |

Pre-existing 실패 2건 (B-4 이래 동일):

- `ShieldApplicationTests.contextLoads` — `DB_URL` placeholder (테스트용 env 미설정)
- `ChecklistCoverageServiceTest` — 형법 체크리스트 커버리지

### DB 쿼리 확인

`group:leasing` + `group:jeonse` 배열 겹침 매칭: **55개 청크**. 임대차 의도
시 이 55개가 soft-우선 후보가 된다.

## 영향 범위

### 변경되지 않은 것

- `LegalRetrievalService` 인터페이스 (5-인자 시그니처는 B-4부터 존재)
- `PgLegalRetrievalService` 구현체
- `StubLegalRetrievalService` (매개변수 받기만 하고 무시)
- `RagContextBuilder`, Layer 3
- `RAG_STUB`, `RAG_HNSW_EF_SEARCH` 등 환경변수

### 변경된 것

- `category-law-mappings.yml` — 7개 L2 노드에 `category_ids` 필드 추가
- `CategoryLawMapping.java` — `categoryIds` 필드 + getter/setter
- `CategoryLawMappingService.java` — `resolveCategoryIds`, `resolveWithL3Fallback`, `parseCategoryIds`
- `MessageService.java` — retrieve 호출을 5-인자로 전환, categoryIds 전달
- `CategoryLawMappingServiceTest.java` — 신규 5건

## 운영 가이드

### 새 group 태그 추가 시

1. 인제스트 스크립트에서 `category_ids`에 새 `group:xxx` 값을 채운다
2. 해당 태그가 의미적으로 매칭되는 온톨로지 L2 노드를 찾아
   `category-law-mappings.yml`의 그 노드에 `category_ids:` 배열로 등록
3. `CategoryLawMappingServiceTest`에 검증 케이스 추가
4. 앱 재기동 (YAML은 PostConstruct에서 1회 로드)

### 비활성화

YAML에서 해당 노드의 `category_ids` 필드를 지우거나 빈 배열(`[]`)로 두면
`resolveCategoryIds`가 빈 리스트를 반환하여 필터가 적용되지 않는다.
코드 변경 불필요.

## B-8 진행 현황

| 하위 항목 | 상태 |
|---|---|
| **B-8a: categoryIds 전달 활성화** | **완료 (본 PR)** |
| B-8b: Prometheus/Micrometer 메트릭 | 보류 |
| B-8c: 한국어 BM25 (pg_bigm 등) | 보류 |
| B-8d: 판례/특별법 인제스트 확장 | 보류 |

B-8a 완료로 B-4에서 준비된 categoryIds soft-filter가 실제 라이브 경로에서 작동한다.
