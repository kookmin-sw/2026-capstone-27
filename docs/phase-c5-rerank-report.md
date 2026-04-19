# Phase C-5 — Cohere Rerank 3.5 도입 효과 측정

현재 베이스라인(하이브리드 SQL only)에 Cohere `rerank-v3.5`를 후단 재정렬로 얹었을 때의 효과를 민법 단일 코퍼스 30 질의 eval set v1로 측정한 결과. 프로덕션 코드(Spring `PgLegalRetrievalService`)는 이번 단계에서 변경하지 않음 — 측정 전용.

## 실험 설계

| 항목 | 값 |
|---|---|
| eval set | `eval/eval-set.v1.jsonl` (민법 30 질의, 41 gold 조문) |
| baseline | 3-way 하이브리드 SQL (vec 0.5 / bm25 0.3 / trig 0.2) top-10 |
| rerank | 동일 SQL로 pool=20 → `rerank-v3.5`로 재정렬 → top-10 |
| 지표 | Recall@1/3/5/10, MRR, nDCG@5 |
| 판정 | `(law_id, article_no)` 튜플 정확 일치 |
| 러너 | `scripts/eval_rag.py [--rerank]` |
| 원시 결과 | `docs/phase-c1-baseline.{md,json}`, `docs/phase-c5-rerank.{md,json}` |

rerank 입력 문서 포맷 (Cohere 권장 YAML-friendly 직렬화):
```
law: {law_name}
article: {article_no}
title: {article_title}
content: {content}
```

## 종합 지표 비교

| 지표 | baseline | rerank | Δ |
|---|---|---|---|
| Recall@1 | 0.6444 | 0.6556 | **+0.0111** |
| Recall@3 | 0.7944 | 0.9111 | **+0.1167** |
| Recall@5 | 0.8944 | 0.9111 | **+0.0167** |
| Recall@10 | 0.9389 | 0.9389 | 0.0000 |
| MRR | 0.8344 | 0.8778 | **+0.0433** |
| nDCG@5 | 0.8150 | 0.8567 | **+0.0417** |

**핵심 관찰**: 가장 큰 개선은 **Recall@3 (+0.117)**. Recall@10은 변동 없음(+0.000) — 즉, rerank는 새로운 정답을 끌어오는 게 아니라 **top-10 안에 이미 있던 정답을 상위권으로 재정렬**하는 효과. 이는 rerank 적용이 pool에 이미 포함된 후보만 다루는 구조와 부합.

Phase C 성공 기준(Recall@5 ≥ 0.85, MRR ≥ 0.65)은 baseline과 rerank 모두 달성.

## 도메인별 변화

| 도메인 | 질의 수 | R@5 base→rerank | MRR base→rerank |
|---|---|---|---|
| contract_general | 3 | 1.000→1.000 | 1.000→1.000 |
| debt_guaranty | 2 | 1.000→1.000 | 1.000→1.000 |
| debt_prescription | 3 | 1.000→1.000 | 1.000→**0.833** (하락) |
| family_divorce | 2 | 1.000→1.000 | 1.000→1.000 |
| family_parental | 2 | 1.000→1.000 | 0.625→**0.750** |
| inheritance | 4 | 0.875→**1.000** | 1.000→1.000 |
| inheritance_reserve | 1 | 0.500→0.500 | 0.500→0.500 |
| misc | 1 | 1.000→1.000 | 1.000→1.000 |
| real_estate_lease | 4 | 0.833→0.833 | 0.812→**1.000** |
| real_estate_rights | 2 | 1.000→1.000 | 0.667→0.667 |
| real_estate_sale | 2 | 0.500→0.500 | 0.500→0.500 |
| real_estate_security | 1 | 0.500→0.500 | 1.000→1.000 |
| tort | 3 | 1.000→1.000 | 0.567→**0.833** |

개선 도메인: `inheritance`(R@5), `family_parental`·`real_estate_lease`·`tort`(MRR).
악화 도메인: `debt_prescription` MRR 1.000→0.833 (C1-Q09 rank 1→2).

## 약점 5건 개별 분석

베이스라인에서 Recall@5 < 1.0이던 5건을 상세히 본다.

### C1-Q01 전세 보증금 — **MRR 0.25 → 1.00 (rank 4→1)**

- gold: 제317조(전세권 소멸과 동시이행), 제318조(경매청구), 제312조의2(증감청구)
- baseline top-3: 제315조, 제316조, 제311조 — 정의·소멸 관련 조문이 상위
- rerank top-3: **제317조**(O), 제310조, 제315조 — 직접 답이 1위로
- 해석: rerank가 "보증금 반환"이라는 질의 의도를 더 정확히 매칭. 다만 제318조·제312조의2는 여전히 상위 5위 밖.

### C1-Q03 저당권 실행 — 변화 없음 (R@5 0.50)

- gold: 제356조(저당권 내용), 제363조(경매청구권)
- baseline: 제356조 rank 1, 제363조 rank 7 → rerank: 제356조 rank 1, 제363조 rank 10
- 해석: 제363조가 pool(20) 안에는 있으나 rerank가 오히려 더 뒤로 밀어냄. "실행·경매" 질의에서 제363조(경매청구권)보다 제360조(피담보채권)·제364조(제3취득자 변제)가 의미적으로 더 가까워 보이는 현상. BM25 가중치 강화나 쿼리 확장으로 대응 필요.

### C1-Q07 유류분 — 변화 없음 (R@5 0.50)

- gold: 제1115조(반환청구), 제1112조(유류분 비율)
- baseline/rerank 모두: 제1117조 rank 1, 제1115조 rank 2, 제1112조 top-10 밖
- 해석: 제1112조가 pool에 없음. 이건 rerank로 해결 불가 — 회수(recall) 문제. 쿼리에 "비율/구성" 어휘 추가 또는 BM25 키워드 보강 필요.

### C1-Q13 매매 하자담보 제척기간 — 변화 없음 (R@5 0.00)

- gold: 제582조(제척기간)
- baseline/rerank 모두 top-10 밖
- 해석: 마찬가지로 회수 문제. 제582조가 pool 20 안에 들어오지 않음. 쿼리에 "6개월·행사기간" 같은 조문 내 어휘가 없어서 벡터가 일반 하자담보 조문(제580·581조)을 더 가깝게 봄.

### C1-Q16 상속포기 — **R@5 0.50 → 1.00 (rank 9→3)**

- gold: 제1041조(포기 방식), 제1019조(고려기간)
- baseline top-3: 제1041조(O), 제1043조, 제1076조 — 제1019조 rank 9
- rerank top-3: 제1041조(O), 제1044조, **제1019조(O)** — 둘 다 상위 3위 안
- 해석: 명확한 개선 케이스. rerank가 "어떻게 해야 하나요"라는 절차 의도를 잘 파악해 기간·방식 조문을 묶어 올림.

## 기타 주목할 질의

| ID | 도메인 | 변화 | 메모 |
|---|---|---|---|
| C1-Q09 | debt_prescription | MRR **-0.50** (rank 1→2) | 제162조→제163조 순서 바뀜. 둘 다 gold이므로 Recall은 동일, 다만 첫 hit가 뒤로 밀린 케이스 — 유의미한 퇴보는 아님 |
| C1-Q20 | family_parental | MRR +0.25 (rank 4→2) | 친권 질의에서 2순위 등장 |
| C1-Q27 | tort | MRR **+0.80** (rank 5→1) | 위자료 질의에서 제751조를 바로 1위로 |

## 비용·지연 영향

| 모드 | 총 시간 | 질의당 | 질의당 Cohere 호출 |
|---|---|---|---|
| baseline | 37.7s | 1.26s | 1 (embed) |
| rerank | 250.5s | 8.35s | 2 (embed + rerank) |

- rerank 적용 시 질의당 **+7.1s** 증가. 대부분은 무료 티어 rate-limit 대응 페이싱(6.5s/질의). 유료 Cohere production 티어에서는 실제 rerank 호출 자체는 수백 ms 수준이므로, **유료 전환 시 질의당 +0.5~1s** 수준으로 줄어들 전망.
- Cohere 무료 티어는 분당 호출 제한이 있어 eval 러너에서 429 back-off + 고정 페이싱을 구현했다.

## 결론과 다음 액션

### 결론
1. **Rerank는 pool 안 재정렬 도구**이지 회수 개선 도구가 아님. Recall@10은 변화 없음이 이를 증명.
2. 대신 **top-3 품질은 크게 개선**(+11.7%p). 사용자가 실제로 보는 상위 결과의 정답률이 높아짐.
3. 30 질의 중 개선 4건, 순위 스왑 1건(퇴보 아님), 변화 없음 25건 — **순개선**.

### 프로덕션 반영 권고
- C-5 Rerank는 **프로덕션 반영 가치 있음**. Spring `PgLegalRetrievalService`에 `CohereRerankClient`와 `pool → rerank → top-K` 파이프라인을 추가하는 별도 이슈로 제안한다.
- 다만 **유료 티어 전환 이후**가 적절. 무료 티어 rate limit로 런타임 사용자 경험이 악화될 수 있음.

### 남은 약점 — Rerank로 해결 불가능한 회수 문제
C1-Q07(제1112조 유류분 비율), C1-Q13(제582조 제척기간), C1-Q03(제363조 경매청구 pool 외)는 **pool 외부에 정답이 있어 rerank 무관**. 후속 개선 후보:
1. 쿼리 확장 (Query Rewriting) — LLM으로 질의를 조문 어휘에 가깝게 바꾸기
2. BM25 가중치 조정 (0.3 → 0.4) — 어휘 매칭 강화
3. 특별법·판례(C-2/C-3) 편입 후 재측정 — gold 분포 자체가 달라질 수 있음

### 후속 순서
C-5 측정 완료. 다음 단계:
- **C-2 특별법 19개 인제스트** 진행 → eval set v2 재라벨링 → baseline과 rerank 모두 재측정
- C-3/C-4 판례 편입 후 동일 사이클 반복
- 최종 C-6 종합 보고서에서 "민법 단일 baseline → +Rerank → +특별법 → +판례 → +Rerank(확장)" 4단계 개선 곡선을 정량화

## 참고

- 베이스라인 상세: [phase-c1-baseline.md](./phase-c1-baseline.md)
- Rerank 상세: [phase-c5-rerank.md](./phase-c5-rerank.md)
- 원 계획서: [phase-c-plan.md](./phase-c-plan.md)
- 러너: [scripts/eval_rag.py](../scripts/eval_rag.py)
