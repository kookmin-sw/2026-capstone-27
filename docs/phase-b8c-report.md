# Phase B-8c — 한국어 BM25 리콜 향상 완료 보고서

작성일: 2026-04-19
브랜치: `feature/issue-A-migrate-rag-to-postgres`
선행: B-4 (3-way 하이브리드), B-7 (PG 경로 운영 전환), B-8b (관측성)

## 목적

B-4에서 도입한 BM25 경로(`to_tsquery('simple', ...)`)가 한국어 코퍼스에서
사실상 제 역할을 하지 못하고 있던 문제를 해결한다. 구체적으로 "전세금이",
"전세금의" 같은 조사가 붙은 실제 문서 토큰이 "전세금" 쿼리로 매칭되지 않아
리콜이 매우 낮았다.

## 사전조사 — Supabase 한국어 FTS 확장 가용성

`pg_available_extensions`를 조회한 결과 Supabase 매니지드 Postgres에서
설치 가능한 관련 확장은 다음뿐이다.

| 확장 | 설치 가능 | 비고 |
|---|---|---|
| `pg_trgm` | O | 이미 설치됨 (trigram 유사도) |
| `vector` | O | 이미 설치됨 (pgvector) |
| `unaccent` | O | 미설치 — 한글에는 효과 없음 |
| `pg_bigm` | **X** | available 목록에 없음 |
| `mecab_ko` | **X** | 미지원 |
| `pg_jieba` | **X** | 미지원 |
| `textsearch_ko` | **X** | 미지원 |

결론: Supabase 환경에서는 **한국어 형태소 분석 기반 BM25가 근본적으로 불가**.
커스텀 regconfig 역시 재료가 되는 dictionary/parser가 없어 의미 있는 효과를
기대하기 어렵다.

## 핵심 원인

`content_tsv`는 V3 마이그레이션에서 `to_tsvector('simple', ...)`로 생성된
GENERATED 컬럼이다. `simple` regconfig은 공백을 기준으로 토큰만 분리하고
어간 분석이나 정규화를 전혀 수행하지 않는다. 한국어에 적용하면:

```
to_tsvector('simple', '전세금이 반환되지 않을 때 전세권자는')
-- → '때':4 '반환되지':2 '않을':3 '전세권자는':5 '전세금이':1
```

즉 "전세금이", "전세권자는" 등이 그대로 독립 토큰으로 저장된다. 쿼리 측에서
`to_tsquery('simple', '전세금')`을 날려도 매칭되지 않는다.

## 대안 설계 비교

| 접근 | 장점 | 단점 | 채택 |
|---|---|---|---|
| (A) pg_bigm 도입 | bi-gram 기반 한국어 부분일치 | Supabase 미지원 | 불가 |
| (B) 커스텀 korean regconfig | 단어 경계 통제 가능 | 파서/사전 없어 효과 미미, 재빌드 비용 | 이연 |
| (C) content_tsv 재생성 + unaccent | 국제화 이점 | 한국어 영향 없음 | 기각 |
| (D) **BM25 prefix 매칭 (`:*`) + trigram GIN 인덱스** | 애플리케이션만 변경, 즉시 리콜 향상 | 접미사 중의성 일부 존재 | **채택** |

## 구현

### 1. BM25 쿼리 prefix 매칭

`PgLegalRetrievalService.buildKeywordTsQuery`에서 각 키워드에 `:*` 접미사를
부여한다. 1글자 토큰은 매칭 폭발을 방지하기 위해 제외한다.

변경 전:
```java
return sanitized.stream().collect(Collectors.joining(" | "));
// → "전세금 | 보증금"
```

변경 후:
```java
return sanitized.stream().map(this::withPrefixMatch).collect(Collectors.joining(" | "));
// → "전세금:* | 보증금:*"

private String withPrefixMatch(String token) {
    return token.length() >= 2 ? token + ":*" : token;
}
```

`simple` regconfig은 prefix 매칭(`:*`)을 기본 지원하므로 `content_tsv`
재생성 없이 즉시 효과를 발휘한다. GIN 인덱스(`idx_legal_chunks_tsv`)도
prefix scan에 대응한다.

### 2. pg_trgm GIN 인덱스 추가 (V5 마이그레이션)

기존 3-way의 trigram 경로(`similarity(content, ...)`)가 seq scan으로
동작할 가능성이 있어 GIN 인덱스를 추가한다.

```sql
-- src/main/resources/db/migration/V5__add_trigram_index_and_korean_bm25_note.sql
CREATE INDEX IF NOT EXISTS idx_legal_chunks_content_trgm
    ON legal_chunks USING gin (content gin_trgm_ops);
```

운영 DB에는 사전 벤치마크 시 수동으로 이미 생성되어 있어 Flyway는 no-op로
처리된다(멱등).

## 벤치마크

### 리콜 향상 (키워드 10종, 1,193행 코퍼스)

| 키워드 | exact | prefix (`:*`) | 배수 |
|---|---:|---:|---:|
| 전세금 | 1 | 6 | 6.0× |
| 계약해지 | 0 | 5 | ∞ |
| 손해배상 | 8 | 47 | 5.9× |
| 임대차 | 3 | 20 | 6.7× |
| 담보 | 0 | 65 | ∞ |
| 채권 | 10 | 149 | 14.9× |
| 전세권 | 4 | 26 | 6.5× |
| 저당권 | 4 | 24 | 6.0× |
| 유치권 | 0 | 10 | ∞ |
| 매매 | 2 | 24 | 12.0× |
| **합계** | **32** | **376** | **11.8×** |

"계약해지", "담보", "유치권"은 exact 매칭이 0인데 prefix 매칭으로 복구된다.
기존 3-way에서는 벡터/trigram이 이를 보완하고 있었지만, 이제 BM25 자체가
본래 역할을 수행한다.

### 품질 — "전세금" 쿼리 상위 5개 (ts_rank)

| 순위 | exact (1건만 반환) | prefix (5건) |
|---|---|---|
| 1 | 민법 제312조의2 (0.0760) | 민법 제312조의2 (0.1368) |
| 2 | — | 민법 제303조 전세권 내용 (0.1216) |
| 3 | — | 민법 제314조 (0.0608) |
| 4 | — | 민법 제315조 (0.0608) |
| 5 | — | 민법 제317조 (0.0608) |

1위가 동일(민법 제312조의2, 전세금 증감청구권)하면서 전세권 본문(제303조)과
관련 조문이 추가되었다. 상위 정밀도를 유지하면서 리콜이 확장됐다.

### trigram 인덱스 활용 (EXPLAIN ANALYZE)

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, similarity(content, '전세금 반환') AS sim
FROM legal_chunks WHERE content % '전세금 반환'
ORDER BY sim DESC LIMIT 10;
```

```
-> Bitmap Heap Scan on legal_chunks
   -> Bitmap Index Scan on idx_legal_chunks_content_trgm
         Index Cond: (content % '전세금 반환'::text)
Execution Time: 1.144 ms
```

seq scan 대신 Bitmap Index Scan 활용 확인. 1,193행 규모에서는 차이가 크지
않지만 인제스트 확장(Phase B-8d / Phase C) 후에도 기존 지연을 유지한다.

## 테스트 결과

`./gradlew test` 기준 **91 passed / 2 failed** (B-8b 이후 유지).
실패 2건은 pre-existing 이슈로 본 변경과 무관.

`PgLegalRetrievalServiceTest.retrieve_3way_생성`의 keywordQuery 검증
(`contains("전세")`)은 `"전세:*"` 형태로도 여전히 통과한다.

## 파일 변경

- `src/main/java/org/example/shield/ai/infrastructure/PgLegalRetrievalService.java`
  * `buildKeywordTsQuery`에 prefix 매칭 적용
  * `withPrefixMatch(String)` 헬퍼 신규
- `src/main/resources/db/migration/V5__add_trigram_index_and_korean_bm25_note.sql` — NEW

## 한계와 후속 작업

- **중의성**: `"전세:*"`는 "전세권자", "전세권설정등기"처럼 의미가 확장된
  토큰도 매칭한다. 현재 3-way 가중치(벡터 0.5, BM25 0.3, trigram 0.2)로
  상위 순위에서는 크게 문제되지 않지만, 추후 re-ranker 도입 시 정렬을
  한 번 더 정리하는 것이 바람직하다.
- **형태소 분석**: 한국어 형태소 분석 기반 BM25는 Supabase 환경에서
  여전히 불가. 자가 호스팅 Postgres로 이전하거나, 애플리케이션 레이어에서
  Nori/Kkma 같은 JVM 형태소 분석기로 키워드를 어간화해 `to_tsquery`에
  넣는 안이 Phase C의 선택지가 될 수 있다.
- **regconfig 변경**: `content_tsv`의 regconfig을 변경하려면 GIN 인덱스
  재빌드가 필요하고, 재빌드 이득이 명확하지 않으므로 본 단계에서는 생략.
