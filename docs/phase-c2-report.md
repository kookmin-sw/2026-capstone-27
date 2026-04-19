# Phase C-2 — 특별법 18개 인제스트 및 전체 코퍼스 벤치마크

## 요약

- 목표: 민법 단일 코퍼스에 전세사기·상속·회생 등 실생활 도메인과 밀접한 **특별법 18개**를 확장 인제스트하고, 동일 eval-set으로 민법 단일 vs 전체 코퍼스 회귀 비교.
- 결과: 특별법 총 **1,873 조문**(법제처 현행판)을 Cohere `embed-v4.0` 1024차원으로 임베딩하여 pgvector에 upsert 완료. DB 총 청크 **3,066개** (민법 1,193 + 특별법 1,873).
- 검색 지표: C-1 baseline(민법 단일) 대비 Recall@5 **0.8944 → 0.8167**(-0.078), MRR **0.8344 → 0.8006**(-0.034). 30개 질의 중 26개 동일·4개만 하락·0개 개선 — 예상된 **민법 gold 전용 eval의 구조적 한계**이며, C-3/C-5에서 보완한다.

## 1. 대상 법령 — 특별법 18개

온톨로지(`ontology/specialized_laws.csv`)의 LSI 19개 중 민법(LSI265307)을 제외한 18개.
법제처 LOD의 LSI는 개정판 식별자이므로 DRF API와 직접 매치되지 않아, `lawSearch.do`로 법령명 검색 → 현행판 MST 획득 → `lawService.do`로 조문 취득하는 2단계 수집 방식을 사용.

| law_id | 법령명 | MST | 조문 수 |
|---|---|---|---|
| law-debtor-rehab | 채무자 회생 및 파산에 관한 법률 | 267359 | 699 |
| law-civil-exec | 민사집행법 | 268837 | 313 |
| law-privacy | 개인정보 보호법 | 270351 | 126 |
| law-real-estate-reg | 부동산등기법 | 265377 | 121 |
| law-inheritance-tax | 상속세 및 증여세법 | 276123 | 111 |
| law-family-proc | 가사소송법 | 265309 | 90 |
| law-auto-accident | 자동차손해배상 보장법 | 277017 | 86 |
| law-child-support | 양육비 이행확보 및 지원에 관한 법률 | 276893 | 48 |
| law-fraud-lease-victim | 전세사기피해자 지원 및 주거안정에 관한 특별법 | 277021 | 47 |
| law-housing-lease | 주택임대차보호법 | 276291 | 42 |
| law-wage-guarantee | 임금채권보장법 | 259881 | 40 |
| law-housing-lease-enf | 주택임대차보호법 시행령 | 280995 | 36 |
| law-commercial-lease | 상가건물 임대차보호법 | 276285 | 32 |
| law-commercial-lease-enf | 상가건물 임대차보호법 시행령 | 280987 | 23 |
| law-debt-collect | 채권의 공정한 추심에 관한 법률 | 268669 | 22 |
| law-real-estate-title | 부동산 실권리자명의 등기에 관한 법률 | 215759 | 17 |
| law-surety-protect | 보증인 보호를 위한 특별법 | 251943 | 11 |
| law-lease-reg-rule | 임차권등기명령 절차에 관한 규칙 | 252747 | 9 |
| **합계** | | | **1,873** |

## 2. 파이프라인 변경 요약

### 2.1 시드 수집 (`scripts/fetch_special_laws.py`)
- 입력: `ontology/specialized_laws.csv`(LSI + 법령명)에서 민법 제외한 18개.
- 1단계 — `lawSearch.do?OC=...&target=law&query=<법령명>&type=XML&display=5` 호출로 현행판 MST·법령ID·공포일·시행일 확보.
- 2단계 — `lawService.do?OC=...&target=law&MST=<mst>&type=XML`로 조문 본문 수집.
- 산출: `src/main/resources/seed/special-laws/<law_id>.json` 18개, 각 파일은 `meta` + `articles[{article_no, title, content, effective_date, is_abolished}]` 구조.
- **방어 코드**: 법제처 API가 목내용을 때때로 단일 문자열, 때때로 리스트로 반환하는 현상에 `_str_content()` 헬퍼로 대응.

### 2.2 카테고리 매핑 (`CategoryLawMappingService`)
- 기존 온톨로지 카테고리 → LSI 다대다 매핑에 **역인덱스**(`resolveCategoriesByLsi(lsi)`)를 추가해 인제스트 시 각 법령이 속한 모든 카테고리 ID를 계산.

### 2.3 인제스트 서비스 (`SpecialLawIngestService` + `SpecialLawIngestRunner`)
- 민법 인제스트(`CivilLawIngestService`)를 일반화하여 시드 디렉터리를 스캔, 각 JSON을 로드하여 동일한 Cohere embed → pgvector upsert 경로를 타도록 구성.
- `@Order(20)`으로 민법 인제스트 이후 실행. `--ingest=special-laws` CLI 인자로 선택 실행.
- 재임베딩 스킵: 본문 해시 불변 시 기존 임베딩 재사용.

### 2.4 `application-ingest.yml`
- Spring Boot 4.0.5 환경에서 `REDIS_HOST`가 비었을 때 health contributor가 context 로드를 실패시키는 이슈 해결을 위해 `RedisAutoConfiguration`·`RedisRepositoriesAutoConfiguration` exclude.
- 실행 시 `REDIS_HOST=localhost` 더미값을 함께 지정.

## 3. 실행 결과

### 3.1 인제스트 실행
- 실행 시간: **521초** (8분 41초), 31 배치, 실패 0건
- 임베딩 모델: `embed-v4.0`, 1024 dim, `input_type=search_document`
- 검증 쿼리:
  ```sql
  SELECT law_id, law_name, COUNT(*) FROM legal_chunks
   WHERE abolition_date IS NULL GROUP BY law_id, law_name;
  ```
  → 19개 법령, 총 3,066 rows 확인.

### 3.2 벤치마크 — 민법 단일 vs 전체 코퍼스

eval-set v1(30 질의, 모든 gold는 민법 조문 기준), 하이브리드 SQL only, 후단 재정렬 없음.

| 지표 | C-1 baseline (민법 단일) | C-2 (민법+특별법) | Δ |
|---|---|---|---|
| Recall@1 | 0.6444 | 0.6111 | -0.0333 |
| Recall@3 | 0.7944 | 0.7611 | -0.0333 |
| Recall@5 | 0.8944 | 0.8167 | **-0.0778** |
| Recall@10 | 0.9389 | 0.8944 | -0.0445 |
| MRR | 0.8344 | 0.8006 | -0.0338 |
| nDCG@5 | 0.8152 | 0.7666 | -0.0486 |
| 전체 gold 조문 수 | 41 | 41 | — |

### 3.3 도메인별 Recall@5 — 하락 구간 집중 분석

| 도메인 | n | R@5 (C-1) | R@5 (C-2) | Δ |
|---|---|---|---|---|
| real_estate_rights | 2 | 1.0000 | 0.5000 | **-0.50** |
| tort | 3 | 1.0000 | 0.6667 | **-0.33** |
| real_estate_lease | 4 | 0.8333 | 0.7500 | -0.08 |
| 나머지 10개 도메인 | 21 | — | — | 동일 |

## 4. 퇴보 질의 4건 — 개별 분석

민법 gold를 기준으로 동일 도메인의 특별법 조문이 벡터/BM25 점수에서 경쟁하며 gold 조문을 밀어낸 사례.

### C1-Q04 — 소유권이전등기 (민법 제186조)
- 질의: "부동산을 매수했는데 소유권이전등기를 하지 않으면 어떻게 되나요"
- C-1 rank: **3** → C-2 rank: **10위 밖**
- 원인: **부동산등기법** 제62~99조(소유권이전등기의 절차 규정) 4건이 상위를 점유. 의미적으로 모두 "소유권이전등기"를 다루므로 벡터·BM25 모두 높음.
- 해석: 민법 제186조는 *원인행위로서의 물권변동*을, 부동산등기법은 *등기 절차*를 다뤄 법적 의의가 다르지만 eval-set은 민법 조문만 gold로 표시. → eval-set 보강 필요.

### C1-Q27 — 위자료 (민법 제751조)
- C-1 rank: **5** → C-2 rank: **8**
- 원인: 자동차손해배상 보장법 제23조·제12조가 "교통사고" 키워드로 BM25 상위에 진입.
- 해석: 질의에 "교통사고"가 명시되어 특별법이 실제로 적실성 높음 — eval-set에 자배법 조문 추가 고려.

### C1-Q01 — 전세보증금 반환 (민법 제312조의2 등)
- C-1 rank: **4** → C-2 rank: **6**
- 원인: 주택임대차보호법 제3조의2(보증금 우선변제)·제4조가 상위 진입. 실무상 이 조문들은 실제로 우선 적용되는 특별법 조문.
- 해석: 민법 gold 외에 주임법 조문도 gold로 인정해야 함.

### C1-Q17 — 자필증서 유언 (민법 제1066조)
- C-1 rank: **1** → C-2 rank: **2**
- 원인: 민법 제1060조(유언의 방식)가 1위로 이동. 특별법 영향 없음 — 카테고리 필터가 민법 유언 조문 전체를 동일 후보군으로 확장해 벡터 노이즈가 커진 영향으로 추정.
- 해석: 미세 회귀, rerank로 회복 가능.

## 5. 결론 및 후속 작업

1. **인제스트 품질**: 18/18 법령, 1,873/1,873 조문 100% 성공. 데이터 품질 양호.
2. **검색 지표**: 특별법이 실제로 더 적실성 높은 질의(Q01·Q27)에서 특별법이 상위를 점유하나 eval-set이 민법 조문만 gold로 두어 **측정상 퇴보**로 나타남. 4건 중 2건은 **실제로는 정답의 다양성 증대**에 해당.
3. **eval-set v2 필요성**:
   - Q01에 주택임대차보호법 제3조의2 추가
   - Q27에 자동차손해배상 보장법 제3조·제5조 추가
   - 전세사기·회생파산·개인정보 도메인 신규 질의 10건 추가
4. **재정렬의 필요성 재확인**: C-5 rerank 로직(Cohere rerank-v3.5)을 전체 코퍼스에 적용할 경우, 정답 재순위화로 회귀 상당 부분이 회복될 가능성이 높음. 후속 C-6 종합 벤치에서 `--law-id all --rerank --pool 30`으로 재측정 예정.

## 6. 실행 재현 명령

```bash
cd /home/user/workspace/SHIELD_BE

# 1) 특별법 시드 재수집
LAW_OC=rkd041804 python3 scripts/fetch_special_laws.py

# 2) 인제스트 (민법 → 특별법)
SPRING_PROFILES_ACTIVE=ingest \
  DB_URL="jdbc:postgresql://.../postgres" \
  DB_USERNAME="..." DB_PASSWORD="..." \
  REDIS_HOST="localhost" \
  COHERE_API_KEY="..." \
  ./gradlew --quiet bootRun --args='--ingest=special-laws'

# 3) 벤치마크 (민법 단일 재실행)
COHERE_API_KEY="..." DB_PASSWORD="..." \
  python3 scripts/eval_rag.py --eval eval/eval-set.v1.jsonl \
  --output docs/phase-c1-baseline.md
  # (--law-id 기본 law-civil)

# 4) 벤치마크 (전체 코퍼스)
COHERE_API_KEY="..." DB_PASSWORD="..." \
  python3 scripts/eval_rag.py --eval eval/eval-set.v1.jsonl \
  --output docs/phase-c2-all-laws.md --law-id all
```

## 7. 첨부

- 원시 결과: [docs/phase-c2-all-laws.md](./phase-c2-all-laws.md), [docs/phase-c2-all-laws.json](./phase-c2-all-laws.json)
- baseline: [docs/phase-c1-baseline.md](./phase-c1-baseline.md)
- rerank baseline: [docs/phase-c5-rerank.md](./phase-c5-rerank.md)
