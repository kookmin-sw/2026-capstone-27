#!/usr/bin/env python3
"""Phase C-1/C-5 자동 벤치 러너.

eval-set JSONL을 읽어 각 질의에 대해 Spring `PgLegalRetrievalService`와 동일한
3-way 하이브리드 SQL을 psql로 실행하고 다음 지표를 계산한다.

- Recall@1, Recall@3, Recall@5, Recall@10
- MRR (첫 gold 조문의 역순위 평균, top-10 미발견 시 0)
- nDCG@5 (gold 이진 관련성)

`--rerank` 플래그를 주면 Cohere `rerank-v3.5`로 후단 재정렬을 적용한다.
후단 재정렬은 하이브리드 SQL로 top-K_pool을 뽑은 뒤, 각 청크를 YAML-friendly
문자열로 직렬화해 rerank API에 넘기고, `relevance_score` 내림차순으로 재정렬한다.

출력:
- Markdown 보고서 (`--output` 경로)
- JSON (Markdown 경로의 .json, 기계 판독용)

사용법:
  COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/eval_rag.py \\
      --eval eval/eval-set.v1.jsonl \\
      --output docs/phase-c1-baseline.md

  COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/eval_rag.py \\
      --eval eval/eval-set.v1.jsonl \\
      --output docs/phase-c5-rerank.md \\
      --rerank --pool 20

설계 메모:
- SQL 가중치와 BM25 prefix 규칙은 Spring 서비스와 동일 (vec 0.5 / bm25 0.3 / trig 0.2).
- hit 판정: (law_id, article_no) 튜플 기준 정확 일치.
- top-K 개수는 MRR/Recall 산정을 위해 10으로 확장한다.
- rerank 모드에서는 후보 pool(기본 20)을 뽑은 뒤 10개로 잘라 같은 지표를 계산한다.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

COHERE_KEY = os.environ["COHERE_API_KEY"]
DB_PASS = os.environ["DB_PASSWORD"]
DB_HOST = "aws-1-ap-northeast-2.pooler.supabase.com"
DB_USER = "postgres.dstngzjsxwzhiwbrzkvy"
DB_NAME = "postgres"

EMBED_MODEL = "embed-v4.0"
EMBED_DIM = 1024
RERANK_MODEL = "rerank-v3.5"
WV, WK, WT = 0.5, 0.3, 0.2
TOPK = 10
RECALL_KS = (1, 3, 5, 10)
NDCG_K = 5


def embed_query(text: str) -> list[float]:
    # embed도 무료 티어에서 429 날 수 있어 공유 _cohere_post 경로 사용
    data = _cohere_post("/v2/embed", {
        "model": EMBED_MODEL,
        "texts": [text],
        "input_type": "search_query",
        "embedding_types": ["float"],
        "output_dimension": EMBED_DIM,
    }, timeout=30)
    return data["embeddings"]["float"][0]


def _cohere_post(path: str, body_dict: dict, timeout: int = 60) -> dict:
    """Cohere API POST with 429 back-off. 무료 티어 rate limit 대응."""
    body = json.dumps(body_dict).encode("utf-8")
    last_err: Exception | None = None
    for attempt in range(6):
        req = urllib.request.Request(
            f"https://api.cohere.com{path}",
            data=body,
            headers={
                "Authorization": f"Bearer {COHERE_KEY}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            last_err = e
            if e.code == 429:
                sleep_sec = min(60, 6 * (attempt + 1))
                print(f"[WARN] 429 on {path}, retrying in {sleep_sec}s (attempt {attempt+1}/6)", file=sys.stderr)
                time.sleep(sleep_sec)
                continue
            raise
    raise last_err  # type: ignore[misc]


def rerank(query: str, documents: list[str], top_n: int) -> list[dict]:
    """Cohere rerank-v3.5 호출. 반환: [{index, relevance_score}, ...]"""
    data = _cohere_post("/v2/rerank", {
        "model": RERANK_MODEL,
        "query": query,
        "documents": documents,
        "top_n": top_n,
    })
    return data["results"]


def to_pgvector_literal(vec: list[float]) -> str:
    return "[" + ",".join(f"{v:.6f}" for v in vec) + "]"


def with_prefix(token: str) -> str:
    """Java PgLegalRetrievalService.withPrefixMatch와 동일 규칙."""
    return token + ":*" if len(token) >= 2 else token


def build_keyword_query(keywords: list[str]) -> str:
    return " | ".join(with_prefix(k) for k in keywords)


def pipe_escape(s: str) -> str:
    """psql -A 출력의 '|' 구분자와 충돌 방지용."""
    return (s or "").replace("|", "/")


def run_search(case: dict, limit: int, law_id_filter: str | None) -> tuple[list[dict], str]:
    qvec = embed_query(case["query"])
    qvec_lit = to_pgvector_literal(qvec)
    kq = build_keyword_query(case["bm25_keywords"])
    categories = case.get("category_ids") or []

    if categories:
        cat_literal = "ARRAY[" + ",".join(f"'{c}'" for c in categories) + "]::text[]"
    else:
        cat_literal = "NULL::text[]"

    # law_id 필터: None이면 전체 코퍼스, 값이 있으면 단일 law_id로 제한.
    # reranker가 쓸 본문(content)과 법령명(law_name)도 SELECT에 포함.
    # 구분자는 |||(삼중 파이프) — 본문에 단일 '|'가 있어도 충돌 없음.
    law_clause = f"AND lc.law_id = '{law_id_filter}'" if law_id_filter else ""
    sql = f"""
        SELECT lc.law_id || '|||' ||
               REPLACE(COALESCE(lc.law_name,''), '|||', '/') || '|||' ||
               lc.article_no || '|||' ||
               REPLACE(COALESCE(lc.article_title,''), '|||', '/') || '|||' ||
               ROUND( (
                   CASE WHEN lc.embedding IS NULL THEN 0
                        ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                   END * {WV}
                 + ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1) * {WK}
                 + similarity(lc.content, $${case["query"]}$$) * {WT}
               )::numeric, 4) || '|||' ||
               ROUND((CASE WHEN lc.embedding IS NULL THEN 0
                           ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                      END)::numeric, 4) || '|||' ||
               ROUND(ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1)::numeric, 4) || '|||' ||
               ROUND(similarity(lc.content, $${case["query"]}$$)::numeric, 4) || '|||' ||
               REPLACE(REPLACE(COALESCE(lc.content,''), E'\\n', ' '), '|||', '/')
          FROM legal_chunks lc
         WHERE lc.abolition_date IS NULL
           {law_clause}
           AND ( COALESCE(CARDINALITY({cat_literal}), 0) = 0
                 OR lc.category_ids && {cat_literal} )
           AND ( lc.content_tsv @@ plainto_tsquery('simple', $${case["query"]}$$)
              OR lc.content_tsv @@ to_tsquery('simple', '{kq}')
              OR lc.content % $${case["query"]}$$
              OR lc.embedding IS NOT NULL )
         ORDER BY (
                   CASE WHEN lc.embedding IS NULL THEN 0
                        ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                   END * {WV}
                 + ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1) * {WK}
                 + similarity(lc.content, $${case["query"]}$$) * {WT}
                 ) DESC
         LIMIT {limit};
    """

    env = os.environ.copy()
    env["PGPASSWORD"] = DB_PASS
    conn = f"host={DB_HOST} port=5432 user={DB_USER} dbname={DB_NAME} sslmode=require"
    result = subprocess.run(
        ["psql", conn, "-A", "-t", "-c", sql],
        env=env, capture_output=True, text=True,
    )
    if result.returncode != 0:
        print(f"[SQL ERROR] {case['id']}: {result.stderr}", file=sys.stderr)
        return [], kq
    rows = []
    for line in result.stdout.strip().split("\n"):
        if not line.strip():
            continue
        parts = line.split("|||")
        if len(parts) >= 9:
            rows.append({
                "law_id": parts[0],
                "law_name": parts[1],
                "article_no": parts[2],
                "title": parts[3],
                "score": parts[4],
                "vec_sim": parts[5],
                "bm25": parts[6],
                "trig": parts[7],
                "content": parts[8],
            })
    return rows, kq


def rerank_rows(query: str, rows: list[dict], top_n: int) -> list[dict]:
    """Cohere rerank-v3.5로 재정렬. 각 row에 'rerank_score' 추가."""
    if not rows:
        return rows
    # YAML-friendly 문자열 (Cohere 권장 포맷). 법조문 구조를 보존.
    documents = []
    for r in rows:
        law = r["law_name"] or r["law_id"]
        title = r["title"] or ""
        content = r["content"] or ""
        # 본문은 너무 길면 rerank가 자동 절단하지만, 명시적으로 4000자 제한.
        if len(content) > 4000:
            content = content[:4000]
        doc = f"law: {law}\narticle: {r['article_no']}\ntitle: {title}\ncontent: {content}"
        documents.append(doc)
    results = rerank(query, documents, top_n)
    reordered = []
    for res in results:
        idx = res["index"]
        row = dict(rows[idx])
        row["rerank_score"] = round(float(res["relevance_score"]), 4)
        reordered.append(row)
    return reordered


def compute_metrics(rows: list[dict], gold: list[dict]) -> dict:
    """gold_articles = [{law_id, article_no}, ...] 기준 hit rank 계산.

    주의: 평가는 상위 K=10 기준으로 수행. rerank 모드에서도 pool 크기와 무관하게
    재정렬 후 상위 10개만 사용.
    """
    rows = rows[:TOPK]
    gold_pairs = {(g["law_id"], g["article_no"]) for g in gold}
    result_pairs = [(r["law_id"], r["article_no"]) for r in rows]

    # 각 gold의 최소 rank (top-K 범위 내)
    gold_ranks: list[int | None] = []
    for g in gold_pairs:
        rank = None
        for idx, pair in enumerate(result_pairs, 1):
            if pair == g:
                rank = idx
                break
        gold_ranks.append(rank)

    hits_at = {}
    for k in RECALL_KS:
        hit = sum(1 for r in gold_ranks if r is not None and r <= k)
        hits_at[k] = hit
    recall_at = {k: (hits_at[k] / len(gold_pairs)) if gold_pairs else 0.0 for k in RECALL_KS}

    first_hit_rank = None
    for idx, pair in enumerate(result_pairs, 1):
        if pair in gold_pairs:
            first_hit_rank = idx
            break
    rr = (1.0 / first_hit_rank) if first_hit_rank else 0.0

    dcg = 0.0
    for idx, pair in enumerate(result_pairs[:NDCG_K], 1):
        if pair in gold_pairs:
            dcg += 1.0 / math.log2(idx + 1)
    ideal_hits = min(len(gold_pairs), NDCG_K)
    idcg = sum(1.0 / math.log2(i + 1) for i in range(1, ideal_hits + 1)) if ideal_hits else 0.0
    ndcg = (dcg / idcg) if idcg else 0.0

    return {
        "gold_ranks": gold_ranks,
        "hits_at": hits_at,
        "recall_at": recall_at,
        "first_hit_rank": first_hit_rank,
        "rr": rr,
        "ndcg5": ndcg,
    }


def load_cases(path: Path) -> list[dict]:
    cases = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            cases.append(json.loads(line))
    return cases


def mean(xs: list[float]) -> float:
    return sum(xs) / len(xs) if xs else 0.0


def render_markdown(cases_result: list[dict], summary: dict, meta: dict) -> str:
    lines: list[str] = []
    title_suffix = " (+ Cohere Rerank 3.5)" if meta["use_rerank"] else ""
    lines.append(f"# Phase C RAG 벤치마크{title_suffix}")
    lines.append("")
    lines.append(f"- eval set: `{meta['eval_path']}` ({meta['n_cases']} 질의)")
    lines.append(f"- 가중치: vector={WV}, keyword(BM25)={WK}, trigram={WT}")
    lines.append(f"- topK: {TOPK} (Recall@K K∈{list(RECALL_KS)}, nDCG@{NDCG_K})")
    lines.append("- BM25 쿼리: prefix 매칭 (`키워드:*`) 적용")
    corpus_desc = meta.get("corpus_desc") or "전체 법령 코퍼스"
    lines.append(f"- 코퍼스: {corpus_desc}")
    if meta["use_rerank"]:
        lines.append(f"- 후단 재정렬: Cohere `{RERANK_MODEL}` (후보 pool={meta['pool']} → top-{TOPK})")
    else:
        lines.append("- 후단 재정렬: 없음 (하이브리드 SQL only)")
    lines.append(f"- 실행 시각: {meta['timestamp']}")
    if meta.get("elapsed_sec") is not None:
        lines.append(f"- 총 실행 시간: {meta['elapsed_sec']:.1f}s ({meta['elapsed_sec']/meta['n_cases']:.2f}s/질의)")
    lines.append("")
    lines.append("## 종합 지표")
    lines.append("")
    lines.append("| 지표 | 값 |")
    lines.append("|---|---|")
    for k in RECALL_KS:
        lines.append(f"| Recall@{k} | {summary['recall_at'][k]:.4f} |")
    lines.append(f"| MRR | {summary['mrr']:.4f} |")
    lines.append(f"| nDCG@{NDCG_K} | {summary['ndcg5']:.4f} |")
    lines.append(f"| 질의 수 | {meta['n_cases']} |")
    lines.append(f"| 전체 gold 조문 수 | {summary['total_gold']} |")
    lines.append("")
    lines.append("## 도메인별 Recall@5 / MRR")
    lines.append("")
    lines.append("| 도메인 | 질의 수 | Recall@5 | MRR |")
    lines.append("|---|---|---|---|")
    for dom, s in sorted(summary["by_domain"].items()):
        lines.append(f"| {dom} | {s['n']} | {s['recall5']:.4f} | {s['mrr']:.4f} |")
    lines.append("")
    lines.append("## 질의별 결과")
    lines.append("")
    lines.append("| ID | 도메인 | 질의 | gold | 첫 hit rank | R@1 | R@3 | R@5 | R@10 | RR | nDCG@5 |")
    lines.append("|---|---|---|---|---|---|---|---|---|---|---|")
    for c in cases_result:
        gold_str = ", ".join(f"{g['article_no']}" for g in c["gold_articles"])
        first_rank = c["metrics"]["first_hit_rank"]
        first_rank_s = str(first_rank) if first_rank else "—"
        r = c["metrics"]["recall_at"]
        query_short = c["query"][:40] + ("…" if len(c["query"]) > 40 else "")
        lines.append(
            f"| {c['id']} | {c['domain']} | {query_short} | {gold_str} | "
            f"{first_rank_s} | {r[1]:.2f} | {r[3]:.2f} | {r[5]:.2f} | {r[10]:.2f} | "
            f"{c['metrics']['rr']:.4f} | {c['metrics']['ndcg5']:.4f} |"
        )
    lines.append("")
    lines.append("## 질의별 상위 5개 조문")
    lines.append("")
    for c in cases_result:
        lines.append(f"### {c['id']} — {c['domain']}")
        lines.append("")
        lines.append(f"- 질의: {c['query']}")
        gold_str = ", ".join(f"{g['law_id']} {g['article_no']}" for g in c["gold_articles"])
        lines.append(f"- gold: {gold_str}")
        lines.append(f"- BM25 tsquery: `{c['bm25_tsquery']}`")
        lines.append("")
        if meta["use_rerank"]:
            lines.append("| # | 조문 | 제목 | rerank | hybrid | vec | bm25 | trig | hit |")
            lines.append("|---|---|---|---|---|---|---|---|---|")
        else:
            lines.append("| # | 조문 | 제목 | score | vec | bm25 | trig | hit |")
            lines.append("|---|---|---|---|---|---|---|---|")
        gold_pairs = {(g["law_id"], g["article_no"]) for g in c["gold_articles"]}
        for i, row in enumerate(c["rows"][:5], 1):
            hit = "O" if (row["law_id"], row["article_no"]) in gold_pairs else ""
            title = pipe_escape(row["title"])[:30]
            if meta["use_rerank"]:
                lines.append(
                    f"| {i} | {row['article_no']} | {title} | {row.get('rerank_score','-')} | "
                    f"{row['score']} | {row['vec_sim']} | {row['bm25']} | {row['trig']} | {hit} |"
                )
            else:
                lines.append(
                    f"| {i} | {row['article_no']} | {title} | {row['score']} | "
                    f"{row['vec_sim']} | {row['bm25']} | {row['trig']} | {hit} |"
                )
        lines.append("")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--eval", required=True, help="eval JSONL 경로")
    parser.add_argument("--output", required=True, help="Markdown 출력 경로 (.md)")
    parser.add_argument("--rerank", action="store_true", help="Cohere rerank-v3.5 후단 재정렬 활성화")
    parser.add_argument("--pool", type=int, default=20, help="rerank 입력 후보 pool 크기 (기본 20)")
    parser.add_argument(
        "--law-id",
        default="law-civil",
        help="코퍼스 필터 law_id. 'all'이면 전체 코퍼스(민법+특별법). 기본 'law-civil' (C-1 baseline 호환).",
    )
    args = parser.parse_args()
    law_id_filter: str | None = None if args.law_id == "all" else args.law_id
    corpus_desc = "전체 법령 코퍼스 (민법+특별법)" if law_id_filter is None else f"{law_id_filter} 단일"

    eval_path = Path(args.eval)
    out_md = Path(args.output)
    out_json = out_md.with_suffix(".json")

    cases = load_cases(eval_path)
    print(f"[INFO] {len(cases)} cases loaded from {eval_path}", file=sys.stderr)
    if args.rerank:
        print(f"[INFO] rerank enabled (pool={args.pool}, model={RERANK_MODEL})", file=sys.stderr)

    # 하이브리드 SQL의 limit — rerank면 pool, 아니면 TOPK
    sql_limit = args.pool if args.rerank else TOPK

    cases_result = []
    per_domain: dict[str, list[dict]] = {}
    total_gold = 0
    t0 = time.time()
    for idx, case in enumerate(cases, 1):
        print(f"[INFO] ({idx}/{len(cases)}) {case['id']} — {case['query'][:30]}…", file=sys.stderr)
        rows, kq = run_search(case, sql_limit, law_id_filter)
        if args.rerank and rows:
            rows = rerank_rows(case["query"], rows, top_n=TOPK)
        # 무료 티어 rate limit(분당 ~10 호출) 회피용 페이싱.
        # rerank 모드: embed 1 + rerank 1 = 질의당 2 call → 6.5s 간격.
        # baseline 모드: embed 1 call → 0s.
        if args.rerank and idx < len(cases):
            time.sleep(6.5)
        metrics = compute_metrics(rows, case["gold_articles"])
        total_gold += len(case["gold_articles"])
        # JSON 출력용: content는 너무 커서 제외
        rows_for_out = [{k: v for k, v in r.items() if k != "content"} for r in rows]
        entry = {
            "id": case["id"],
            "domain": case["domain"],
            "query": case["query"],
            "gold_articles": case["gold_articles"],
            "bm25_tsquery": kq,
            "rows": rows_for_out,
            "metrics": metrics,
        }
        cases_result.append(entry)
        per_domain.setdefault(case["domain"], []).append(entry)
    elapsed = time.time() - t0

    summary = {
        "recall_at": {k: mean([c["metrics"]["recall_at"][k] for c in cases_result]) for k in RECALL_KS},
        "mrr": mean([c["metrics"]["rr"] for c in cases_result]),
        "ndcg5": mean([c["metrics"]["ndcg5"] for c in cases_result]),
        "total_gold": total_gold,
        "by_domain": {
            dom: {
                "n": len(items),
                "recall5": mean([c["metrics"]["recall_at"][5] for c in items]),
                "mrr": mean([c["metrics"]["rr"] for c in items]),
            }
            for dom, items in per_domain.items()
        },
    }

    meta = {
        "eval_path": str(eval_path),
        "n_cases": len(cases),
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S %Z"),
        "use_rerank": bool(args.rerank),
        "pool": args.pool if args.rerank else None,
        "elapsed_sec": elapsed,
        "law_id_filter": law_id_filter,
        "corpus_desc": corpus_desc,
    }

    md = render_markdown(cases_result, summary, meta)
    out_md.parent.mkdir(parents=True, exist_ok=True)
    out_md.write_text(md, encoding="utf-8")

    out_json.write_text(
        json.dumps({"meta": meta, "summary": summary, "cases": cases_result}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"[OK] Markdown: {out_md}", file=sys.stderr)
    print(f"[OK] JSON:     {out_json}", file=sys.stderr)
    print(
        f"[SUMMARY] R@1={summary['recall_at'][1]:.3f} R@3={summary['recall_at'][3]:.3f} "
        f"R@5={summary['recall_at'][5]:.3f} R@10={summary['recall_at'][10]:.3f} "
        f"MRR={summary['mrr']:.3f} nDCG@5={summary['ndcg5']:.3f} "
        f"elapsed={elapsed:.1f}s",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
