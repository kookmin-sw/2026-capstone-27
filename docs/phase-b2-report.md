# Phase B-2 완료 보고서 — 민법 인제스트 파이프라인

**일시**: 2026-04-19
**브랜치**: `feature/issue-A-migrate-rag-to-postgres`
**커밋**: `0c0f4b9` — feat(ai): Phase B-2 — 민법 인제스트 파이프라인 완성

## 1. 결과 요약

| 지표 | 값 |
|---|---|
| 총 조문 수 | **1,193** |
| 적재된 row 수 | **1,193** (100%) |
| embedding NOT NULL | **1,193** (100%) |
| category_ids NOT NULL | **1,193** (100%) |
| 임베딩 모델 | `embed-v4.0` (Cohere free trial) |
| 벡터 차원 | **1024** (Matryoshka reduction) |
| 실행 시간 | **337초** (13 batches × 25s avg, batch 96) |
| 실패 건수 | **0** |
| 단위 테스트 | **12/12 통과** |

## 2. 산출물

### 신규 소스
- `scripts/generate_civil_law_category_map.py` — 책/장/절 자동 분류 YAML 생성기
- `src/main/resources/seed/civil-law-category-map.yml` — 5편 32장 67절 7그룹
- `src/main/java/.../ai/config/CivilLawCategoryMap.java` — YAML 로더
- `src/main/java/.../ai/dto/CivilLawSeed.java` — seed JSON 레코드
- `src/main/java/.../ai/application/CivilLawIngestService.java` — 메인 오케스트레이터
- `src/main/java/.../ai/application/CivilLawUpsertService.java` — 트랜잭션 분리 빈
- `src/main/java/.../ai/application/CivilLawIngestRunner.java` — `--ingest=civil-law` 진입점
- `src/main/java/.../common/config/AuditingConfig.java` — OffsetDateTime 프로바이더
- `src/main/resources/application-ingest.yml` — 인제스트 전용 프로필
- `docs/reference/lod/` — LOD 4종 참조 보관 + README (RAG 미사용 사유)
- 테스트 3종: `CivilLawCategoryMapTest`, `CivilLawIngestServiceTest`, `CivilLawUpsertServiceTest`

### 수정
- `ShieldApplication.java` — `@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")`
- `build.gradle` — `jackson-dataformat-yaml` 의존성 추가
- `src/main/resources/seed/civil-law.json` — 가지조문 article_no 보정 (75건)
- `.gitignore` — backup 파일 제외

## 3. 해결한 엔지니어링 이슈

| # | 증상 | 원인 | 해결 |
|---|---|---|---|
| 1 | Cohere는 성공, DB에 0건 | `@Transactional protected` 메서드의 같은 클래스 내 호출 → 프록시 미경유 | 별도 `@Service`(`CivilLawUpsertService`)로 분리 |
| 2 | 2회차 실행 시 429 Too Many Requests | Cohere Trial 요율 제한 (분당 ~40 요청) | `batchDelayMs=1500` (배치 간 1.5초 간격) |
| 3 | `Cannot convert LocalDateTime to OffsetDateTime` | JPA Auditing 기본 provider가 `LocalDateTime`만 반환, 엔티티는 `OffsetDateTime` | `DateTimeProvider` 빈 + `dateTimeProviderRef` 지정 |
| 4 | 1,193건 중 1,118건만 적재 (75건 손실) | seed JSON에서 가지조문(제N조의M)의 `article_no`가 본조(제N조)와 동일 → Unique Key 충돌로 병합 | 순번 기반 `제N조의M` 자동 보정 후 재인제스트 |

## 4. 검증 샘플

**핵심 조문 카테고리 매핑 (기대 ≡ 실측):**

| 조 | 제목 | category_ids |
|---|---|---|
| 제211조 | 소유권의 내용 | `book:제2편 물권 | chapter:제3장 소유권 | section:제1절 소유권의 한계 | group:ownership` |
| 제303조 | 전세권의 내용 | `book:제2편 물권 | chapter:제6장 전세권 | group:jeonse` |
| 제356조 | 저당권의 내용 | `book:제2편 물권 | chapter:제9장 저당권 | group:mortgage` |
| 제387조 | 이행기와 이행지체 | `book:제3편 채권 | chapter:제1장 총칙 | section:제2절 채권의 효력 | group:claim_effect` |
| 제408조 | 분할채권관계 | `book:제3편 채권 | chapter:제1장 총칙 | section:제3절 수인의 채권자 및 채무자 | group:guaranty_debtors` |
| 제618조 | 임대차의 의의 | `book:제3편 채권 | chapter:제2장 계약 | section:제7절 임대차 | group:leasing` |

**가지조문 정상 저장 (10건 샘플):** 제14조의2(특정후견), 제14조의3(심판 사이의 관계), 제52조의2(직무집행정지), 제60조의2(직무대행자), 제289조의2(구분지상권), 제312조의2(전세금 증감), 제428조의2(보증의 방식), 제428조의3(근보증), 제436조의2(정보제공의무), 제674조의2(여행계약) — 전부 실제 민법과 일치.

## 5. 재현 명령

```bash
# 빌드
cd /home/user/workspace/SHIELD_BE && ./gradlew bootJar

# 환경 변수 지정 후 인제스트 실행
DB_URL=jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres \
DB_USERNAME=postgres.dstngzjsxwzhiwbrzkvy \
DB_PASSWORD=*** \
COHERE_API_KEY=*** \
GMAIL_USERNAME=dummy@gmail.com GMAIL_APP_PASSWORD=dummy \
SPRING_PROFILES_ACTIVE=ingest \
java -jar build/libs/shield-0.0.1-SNAPSHOT.jar --ingest=civil-law

# 검증
psql "host=... sslmode=require" \
  -c "SELECT COUNT(*) FROM legal_chunks WHERE law_id='law-civil';"
```

## 6. Phase B 전체 현황

| 단계 | 상태 |
|---|---|
| B-6 R-17 rerun | 완료 |
| B-2 설계 메모 (`docs/phase-b-plan.md`) | 완료 |
| B-1 V4 임베딩 스키마 + Cohere 헬퍼 | 완료 |
| 민법 seed 수집 | 완료 (1,193건) |
| **B-2 민법 인제스트 파이프라인** | **완료** |
| B-4 3-way 하이브리드 검색 (벡터 + BM25 + 카테고리) | 대기 |
| B-5 Redis 캐시 + HNSW 튜닝 | 대기 |
| B-7 `RAG_STUB=false` 전환 | 대기 |

## 7. 다음 단계

다음은 **B-4 (3-way 하이브리드 검색)**:
- 코사인 벡터 검색 (HNSW 인덱스 활용)
- BM25 full-text 검색 (`content_tsv` GIN 인덱스 활용)
- `category_ids` 필터 기반 1차 축소
- 3-way 점수 융합 (RRF 또는 가중합)
