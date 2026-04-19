#!/usr/bin/env python3
"""Phase C-1 자동 벤치 러너.

eval-set JSONL을 읽어 각 질의에 대해 Spring `PgLegalRetrievalService`와 동일한
3-way 하이브리드 SQL을 psql로 실행하고 다음 지표를 계산한다.

- Recall@1, Recall@3, Recall@5, Recall@10
- MRR (첫 gold 조문의 역순위 평균, top-10 미발견 시 0)
- nDCG@5 (gold 이진 관련성)

출력:
- Markdown 보고서 (기본 docs/phase-c1-baseline.md)
- JSON (Markdown 경로의 .json, 기계 판독용)

사용법:
  COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/eval_rag.py \\
      --eval eval/eval-set.v1.jsonl \\
      --output docs/phase-c1-baseline.md

설계 메모:
- SQL 가중치와 BM25 prefix 규칙은 `rag_qualitative_smoke.py`와 동일하게 유지
  (Phase B-8c 반영). 가중치: vec 0.5 / bm25 0.3 / trigram 0.2.
- hit 판정: (law_id, article_no) 튜플 기준 정확 일치.
- top-K 개수는 MRR/Recall 산정을 위해 10으로 확장한다.
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
WV, WK, WT = 0.5, 0.3, 0.2
TOPK = 10
RECALL_KS = (1, 3, 5, 10)
NDCG_K = 5


def embed_query(text: str) -> list[float]:
    body = json.dumps({
        "model": EMBED_MODEL,
        "texts": [text],
        "input_type": "search_query",
        "embedding_types": ["float"],
        "output_dimension": EMBED_DIM,
    }).encode("utf-8")
    req = urllib.request.Request(
        "https://api.cohere.com/v2/embed",
        data=body,
        headers={
            "Authorization": f"Bearer {COHERE_KEY}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    return data["embeddings"]["float"][0]


def to_pgvector_literal(vec: list[float]) -> str:
    return "[" + ",".join(f"{v:.6f}" for v in vec) + "]"


def with_prefix(token: str) -> str:
    """Java PgLegalRetrievalService.withPrefixMatch와 동일 규칙."""
    return token + ":*" if len(token) >= 2 else token


def build_keyword_query(keywords: list[str]) -> str:
    return " | ".join(with_prefix(k) for k in keywords)


def run_search(case: dict) -> tuple[list[dict], str]:
    qvec = embed_query(case["query"])
    qvec_lit = to_pgvector_literal(qvec)
    kq = build_keyword_query(case["bm25_keywords"])
    categories = case.get("category_ids") or []

    if categories:
        cat_literal = "ARRAY[" + ",".join(f"'{c}'" for c in categories) + "]::text[]"
    else:
        cat_literal = "NULL::text[]"

    # law_id는 현재 민법 단일 코퍼스이므로 'law-civil' 고정.
    # gold_articles의 law_id와 대조하기 위해 SELECT에 law_id 포함.
    sql = f"""
        SELECT lc.law_id || '|' || lc.article_no || '|' ||
               COALESCE(lc.article_title,'') || '|' ||
               ROUND( (
                   CASE WHEN lc.embedding IS NULL THEN 0
                        ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                   END * {WV}
                 + ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1) * {WK}
                 + similarity(lc.content, $${case["query"]}$$) * {WT}
               )::numeric, 4) || '|' ||
               ROUND((CASE WHEN lc.embedding IS NULL THEN 0
                           ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                      END)::numeric, 4) || '|' ||
               ROUND(ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1)::numeric, 4) || '|' ||
               ROUND(similarity(lc.content, $${case["query"]}$$)::numeric, 4)
          FROM legal_chunks lc
         WHERE lc.abolition_date IS NULL
           AND lc.law_id = 'law-civil'
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
         LIMIT {TOPK};
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
        parts = line.split("|")
        if len(parts) >= 7:
            rows.append({
                "law_id": parts[0],
                "article_no": parts[1],
                "title": parts[2],
                "score": parts[3],
                "vec_sim": parts[4],
                "bm25": parts[5],
                "trig": parts[6],
            })
    return rows, kq


def compute_metrics(rows: list[dict], gold: list[dict]) -> dict:
    """gold_articles = [{law_id, article_no}, ...] 기준 hit rank 계산."""
    gold_pairs = {(g["law_id"], g["article_no"]) for g in gold}
    # rank(1-based) per gold: None if not in top-K
    result_pairs = [(r["law_id"], r["article_no"]) for r in rows]

    # 각 gold의 최소 rank
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

    # MRR: 전체 질의에서는 "첫 번째 gold의 rank" 기준 역수
    first_hit_rank = None
    for idx, pair in enumerate(result_pairs, 1):
        if pair in gold_pairs:
            first_hit_rank = idx
            break
    rr = (1.0 / first_hit_rank) if first_hit_rank else 0.0

    # nDCG@5 (gold=1, else=0)
    dcg = 0.0
    for idx, pair in enumerate(result_pairs[:NDCG_K], 1):
        if pair in gold_pairs:
            dcg += 1.0 / math.log2(idx + 1)
    # IDCG: gold 개수와 NDCG_K 중 작은 값만큼 상위에 배치된 이상적 순서
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
    lines.append("# Phase C-1 RAG 벤치마크 베이스라인")
    lines.append("")
    lines.append(f"- eval set: `{meta['eval_path']}` ({meta['n_cases']} 질의)")
    lines.append(f"- 가중치: vector={WV}, keyword(BM25)={WK}, trigram={WT}")
    lines.append(f"- topK: {TOPK} (Recall@K K∈{list(RECALL_KS)}, nDCG@{NDCG_K})")
    lines.append("- BM25 쿼리: prefix 매칭 (`키워드:*`) 적용")
    lines.append("- 코퍼스: 민법 (law-civil) 단일")
    lines.append(f"- 실행 시각: {meta['timestamp']}")
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
        lines.append("| # | 조문 | 제목 | score | vec | bm25 | trig | hit |")
        lines.append("|---|---|---|---|---|---|---|---|")
        gold_pairs = {(g["law_id"], g["article_no"]) for g in c["gold_articles"]}
        for i, row in enumerate(c["rows"][:5], 1):
            hit = "O" if (row["law_id"], row["article_no"]) in gold_pairs else ""
            title = (row["title"] or "").replace("|", "/")[:30]
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
    args = parser.parse_args()

    eval_path = Path(args.eval)
    out_md = Path(args.output)
    out_json = out_md.with_suffix(".json")

    cases = load_cases(eval_path)
    print(f"[INFO] {len(cases)} cases loaded from {eval_path}", file=sys.stderr)

    cases_result = []
    per_domain: dict[str, list[dict]] = {}
    total_gold = 0
    for idx, case in enumerate(cases, 1):
        print(f"[INFO] ({idx}/{len(cases)}) {case['id']} — {case['query'][:30]}…", file=sys.stderr)
        rows, kq = run_search(case)
        metrics = compute_metrics(rows, case["gold_articles"])
        total_gold += len(case["gold_articles"])
        entry = {
            "id": case["id"],
            "domain": case["domain"],
            "query": case["query"],
            "gold_articles": case["gold_articles"],
            "bm25_tsquery": kq,
            "rows": rows,
            "metrics": metrics,
        }
        cases_result.append(entry)
        per_domain.setdefault(case["domain"], []).append(entry)

    # 종합 지표
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
        f"MRR={summary['mrr']:.3f} nDCG@5={summary['ndcg5']:.3f}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
