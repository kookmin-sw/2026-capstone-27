# Phase C-4 — 판례 인제스트 및 RAG 벤치마크 보고서

- 브랜치: `feature/issue-A-migrate-rag-to-postgres`
- 작성일: 2026-04-19 (KST)
- Eval set: `eval/eval-set.v1.5.jsonl` (40 질의 = C1 법령 30 + C4 판례 10)
- Corpus:
  - 법령: `legal_chunks` (민법 + 특별법 1,873 조문)
  - 판례: `legal_cases` 183건 (모두 대법원)
- Retrieval: pgvector cosine + tsvector BM25 + pg_trgm, 가중합 스코어
- Embedding: Cohere `embed-multilingual-v3.0` (1024d)

## 1. 핵심 결과 요약

| 지표 | C-2 Baseline (법령만) | C-4 (법령 + 판례) | Δ |
| --- | --- | --- | --- |
| Recall@1 | 0.458 | **0.708** | +0.250 |
| Recall@3 | 0.571 | **0.821** | +0.250 |
| Recall@5 | 0.613 | **0.863** | +0.250 |
| Recall@10 | 0.671 | **0.896** | +0.225 |
| MRR | 0.601 | **0.847** | +0.246 |
| nDCG@5 | 0.575 | **0.825** | +0.250 |
| 평균 elapsed | 57.7s | 71.3s | +13.6s |

- 판례 10건(C4-Q01~10) 전부 **rank 1** 적중 → C4 그룹만 보면 R@1=1.000, MRR=1.000.
- 법령 30건(C1-Q01~30)은 판례를 추가해도 품질 **거의 동일** (R@5 0.817→0.817, MRR 0.801→0.796). 판례가 법령 질의의 상위 결과를 침범하지 않는다는 뜻.
- 평균 지연은 질의당 약 +0.34초. `legal_chunks` UNION `legal_cases` 단일 라운드트립 + 동일 Cohere 임베딩 1회라 비용이 선형보다 작다.

## 2. C1 (법령 gold) vs C4 (판례 gold) 분할

| 그룹 | N | 지표 | Baseline | C-4 | Δ |
| --- | --- | --- | --- | --- | --- |
| **C1-Q** (법령 30) | 30 | R@1 | 0.611 | 0.611 | 0.000 |
| | | R@5 | 0.817 | 0.817 | 0.000 |
| | | R@10 | 0.894 | 0.861 | −0.033 |
| | | MRR | 0.801 | 0.796 | −0.005 |
| | | nDCG@5 | 0.767 | 0.767 | 0.000 |
| **C4-Q** (판례 10) | 10 | R@1 | 0.000 | **1.000** | +1.000 |
| | | R@5 | 0.000 | **1.000** | +1.000 |
| | | MRR | 0.000 | **1.000** | +1.000 |
| | | nDCG@5 | 0.000 | **1.000** | +1.000 |

R@10만 −0.033 (=1건) 하락하는데, 판례가 전체 10위 안에서 법령 조문 일부를 밀어낸 케이스로 보인다. 상위 5위 지표(R@5/nDCG@5)가 동일한 점이 훨씬 중요.

## 3. 판례 hit 상세 (10/10)

| Query ID | 도메인 | Gold case_no | Baseline rank | C-4 rank |
| --- | --- | --- | --- | --- |
| C4-Q01 | real_estate_lease | 2025다213466 | — | **1** |
| C4-Q02 | real_estate_ownership | 2025다210092, 210093 | — | **1** |
| C4-Q03 | damages_prescription | 2025므10716 | — | **1** |
| C4-Q04 | damages_prescription | 2025다211537, 211538 | — | **1** |
| C4-Q05 | family_divorce | 2025스595 | — | **1** |
| C4-Q06 | family_divorce | 2024스876 | — | **1** |
| C4-Q07 | family_divorce | 2025므10730 | — | **1** |
| C4-Q08 | tort | 2025다204730 | — | **1** |
| C4-Q09 | damages_prescription | 2024다254387 | — | **1** |
| C4-Q10 | real_estate_ownership | 2023다316363 | — | **1** |

모든 판례가 단일 pass 내 1위로 올라왔다. 이는

1. 본문이 길고 쟁점이 명확(대법원 판결요지 + 판결이유) → 임베딩 품질이 좋음,
2. 질의가 “임차인이 살던 집을 매수…”처럼 판례 요지와 어휘가 직접적으로 겹침,
3. 판례 수가 183건으로 적어서 같은 도메인 안에서 경쟁이 낮음
   이라는 세 요인이 겹친 결과로 해석된다.

## 4. 도메인별 비교 (R@5 / MRR)

| 도메인 | N | R@5 base → C-4 | MRR base → C-4 |
| --- | --- | --- | --- |
| contract_general | 3 | 1.000 → 1.000 | 1.000 → 1.000 |
| **damages_prescription** | 3 | **0.000 → 1.000 (+1.000)** | **0.000 → 1.000 (+1.000)** |
| debt_guaranty | 2 | 1.000 → 1.000 | 1.000 → 1.000 |
| debt_prescription | 3 | 1.000 → 1.000 | 1.000 → 1.000 |
| **family_divorce** | 5 | **0.400 → 1.000 (+0.600)** | **0.400 → 1.000 (+0.600)** |
| family_parental | 2 | 1.000 → 1.000 | 0.625 → 0.625 |
| inheritance | 4 | 0.875 → 0.875 | 0.875 → 0.875 |
| inheritance_reserve | 1 | 0.500 → 0.500 | 0.500 → 0.500 |
| misc | 1 | 1.000 → 1.000 | 1.000 → 1.000 |
| **real_estate_lease** | 5 | **0.600 → 0.800 (+0.200)** | 0.633 → 0.825 (+0.192) |
| **real_estate_ownership** | 2 | **0.000 → 1.000 (+1.000)** | 0.000 → 1.000 (+1.000) |
| real_estate_rights | 2 | 0.500 → 0.500 | 0.500 → 0.500 |
| real_estate_sale | 2 | 0.500 → 0.500 | 0.500 → 0.500 |
| real_estate_security | 1 | 0.500 → 0.500 | 1.000 → 1.000 |
| **tort** | 4 | **0.500 → 0.750 (+0.250)** | 0.406 → 0.625 (+0.219) |

개선이 크게 나타난 도메인은 모두 **판례 gold가 포함된 도메인**이다. 법령 gold만 있는 도메인(예: inheritance, contract_general, debt_*)은 변화가 없다 — 판례가 법령 검색 품질을 저해하지 않았다는 증거.

## 5. 판례 코퍼스 구성

- 총 **183건** (`src/main/resources/seed/cases/*.json`)
- 모두 대법원 판결 (법제처 OpenAPI 제약: 전문 제공은 대법원만)
- 기간: 1963-06-20 ~ 2026-02-12
- 수집 키워드: 52개 (원래 37 + 보강 16 — 임대차 갱신, 확정일자, 우선변제권, 저당권, 가등기, 해제조건, 이행불능, 공동불법행위, 상속회복청구, 유책배우자, 양자 파양, 채권자 취소권, 부당이득, 조정조서, 무효 설정등기 등)
- 수집 파라미터: `--per-query 30 --supreme-only --min-holding-len 30`
- 인제스트 결과: `embedded=183, upserted=183, failed=0, elapsedMs=55,693`

## 6. 변경 범위

### Python
- `scripts/fetch_cases.py` — 법제처 OpenAPI로 판례 본문 + 판시사항 + 판결요지 + 참조조문 수집. `_parse_cited_articles`가 "민법 제766조 제1항" 같은 조항 중첩 참조를 조 재사용으로 복원.
- `scripts/eval_rag.py`:
  - `--include-cases` 플래그 추가
  - UNION ALL SQL: `({law_sql}) UNION ALL ({case_sql})` — 각 서브쿼리가 ORDER BY/LIMIT을 가지므로 반드시 괄호
  - 13필드 파이프 출력 (kind, law/case 공통)
  - `_gold_key` / `_row_key`에 `kind=='case' → ('case', case_no)` 분기
  - **`_sanitize_keyword`** 추가: `to_tsquery`에서 공백·`&|:!()` 제거 (예: "전 소유자" → "전소유자")
  - Rerank 포맷: 법령 / 판례 분기 (판례는 `type: case\ncourt / case_no / holding`)

### Java
- `src/main/java/org/example/shield/ai/dto/LegalCaseSeed.java` — `meta` + `case` 2층 record
- `src/main/java/org/example/shield/ai/application/LegalCaseUpsertService.java` — `@Transactional`, 자연키 `(case_no, court, decision_date)` 기반 upsert
- `src/main/java/org/example/shield/ai/application/LegalCaseIngestService.java` — 임베딩 입력 조립: `caseName + headnote + holding + reasoning(잔여 공간)`, 최대 4000자. Cohere `embed-multilingual-v3.0` 호출.
- `src/main/java/org/example/shield/ai/application/LegalCaseIngestRunner.java` — `@Profile("ingest")`, `@Order(30)`, `--ingest=cases` 트리거.

### Data
- `src/main/resources/seed/cases/*.json` — 183개
- `eval/eval-set.v1.5.jsonl` — 기존 C1-Q01~30 유지 + C4-Q01~10 판례 gold 10건 추가. `gold_articles[*].kind = "case"` / `case_no` 스키마.

### Docs
- `docs/phase-c4-baseline-laws-only.md` / `.json` — C-2 재실행 결과
- `docs/phase-c4-with-cases.md` / `.json` — C-4 결과
- `docs/phase-c4-report.md` — 이 문서

## 7. 주요 기술적 발견

1. **법제처 판례 API 제약**: 대법원 판결만 본문 전문을 반환한다. 하급심·헌재는 목록만 제공되어 배제(`--supreme-only`). 30년 내 대법원 `case_no`는 유일하므로 gold 매칭은 `case_no`만으로 충분.
2. **UNION ALL 구문**: 각 서브 SELECT에 ORDER BY/LIMIT이 있으면 괄호가 필수. `(SELECT ... LIMIT k) UNION ALL (SELECT ... LIMIT k)` 이후 외부에서 재정렬.
3. **tsquery 공백 이슈**: eval v1.5의 `bm25_keywords`에 "전 소유자", "민법 제766조"처럼 공백 포함 토큰이 있으면 `to_tsquery` syntax error. eval-set 쪽도 공백 제거하고 스크립트 측에도 `_sanitize_keyword` 방어 로직을 이중으로 배치.
4. **판례 임베딩 입력 우선순위**: `caseName + headnote + holding`은 항상 포함, `reasoning`은 남은 공간에만(4000자 상한). 판결요지(holding)가 검색 키워드를 가장 많이 담고 있어 rank 1 적중률에 결정적.
5. **비용 vs 품질**: 판례 183건 추가로 +13.6초/40 질의 = 질의당 +0.34초. 품질 상승 대비 매우 저렴.

## 8. 한계와 다음 단계

- **판례 수 183건**: 10개 gold 모두 rank 1 적중은 코퍼스가 얇아 경쟁이 적은 점이 기여. 1000건 이상으로 늘리면 쉬운 적중이 줄어들 가능성. 다음 단계에서 `--per-query` 상향 및 키워드 추가.
- **하급심 부재**: 대법원만 있어 사실관계 중심 질의 커버리지가 제한적. 법제처 API 외 경로(법원 판결문 검색) 검토 필요.
- **C1 R@10 −0.033**: 판례가 법령 10위 이내를 밀어낸 케이스가 1건. rerank가 해결할 가능성이 높고 현재 rerank는 판례/법령 프롬프트가 분기되어 있어 실험 가능.
- **판례 업데이트 주기**: `fetch_cases.py`를 주 1회 cron으로 돌리고 upsert로 반영하는 운영 파이프라인 필요.

## 9. 권장 결론

- **Phase C-4는 RAG 품질 측면에서 단일 변경 대비 최대 개선 (MRR +0.246, nDCG@5 +0.250)**.
- 법령 retrieval에 부작용이 거의 없으므로 기본 retrieval 경로에 `include_cases=true`로 스위치 가능.
- 다음 단계는 (a) 판례 코퍼스 확장, (b) rerank on/off A/B, (c) 하급심 판결문 경로 추가.
