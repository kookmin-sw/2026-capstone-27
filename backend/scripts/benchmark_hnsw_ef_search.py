#!/usr/bin/env python3
"""Phase B-5 HNSW ef_search 튜닝 벤치마크.

동일 쿼리를 ef_search 값만 바꿔가며 실행해 상위 K 결과와 지연시간을 비교한다.

- 쿼리 임베딩은 Cohere embed-v4.0을 사용 (1024차원)
- HNSW 인덱스: idx_legal_chunks_embedding_hnsw (cosine)
- 측정 방법: psql \\timing on 대신 EXPLAIN (ANALYZE) execution time 파싱

사용법:
  COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/benchmark_hnsw_ef_search.py
"""
import json
import os
import re
import statistics
import subprocess
import sys
import urllib.request

COHERE_KEY = os.environ["COHERE_API_KEY"]
DB_PASS = os.environ["DB_PASSWORD"]
DB_HOST = "aws-1-ap-northeast-2.pooler.supabase.com"
DB_USER = "postgres.dstngzjsxwzhiwbrzkvy"
DB_NAME = "postgres"

EMBED_MODEL = "embed-v4.0"
EMBED_DIM = 1024
TOPK = 10
EF_VALUES = [10, 40, 80, 160, 400]  # 10은 기본 미만, 400은 오버킬 상한
REPEAT = 5  # 각 ef 값마다 5회 반복하여 median 지연 측정

QUERIES = [
    "전세 계약 종료 후 집주인이 보증금을 돌려주지 않음",
    "임차인이 차임을 계속 연체하여 임대차 계약을 해지하고 싶음",
    "채무자가 돈을 갚지 않아 담보로 잡은 부동산에 설정한 저당권을 실행하려 함",
    "부동산을 매수했는데 소유권이전등기를 하지 않으면 어떻게 되는가",
]


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


def run_psql(sql: str) -> str:
    env = os.environ.copy()
    env["PGPASSWORD"] = DB_PASS
    conn = f"host={DB_HOST} port=5432 user={DB_USER} dbname={DB_NAME} sslmode=require"
    result = subprocess.run(
        ["psql", conn, "-v", "ON_ERROR_STOP=1", "-X", "-q", "-c", sql],
        env=env,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        print("SQL ERROR:", result.stderr, file=sys.stderr)
        raise SystemExit(1)
    return result.stdout


def benchmark_one(qvec_lit: str, ef: int) -> tuple[float, list[str]]:
    """EXPLAIN ANALYZE로 실행 시간 + 상위 K 조문번호를 얻는다."""
    # Step 1: EXPLAIN ANALYZE로 실행 시간 측정
    explain_sql = f"""
        BEGIN;
        SET LOCAL hnsw.ef_search = {ef};
        EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
        SELECT lc.article_no,
               1 - (lc.embedding <=> '{qvec_lit}'::vector) AS sim
          FROM legal_chunks lc
         WHERE lc.abolition_date IS NULL
           AND lc.embedding IS NOT NULL
         ORDER BY lc.embedding <=> '{qvec_lit}'::vector
         LIMIT {TOPK};
        ROLLBACK;
    """
    raw = run_psql(explain_sql)
    # JSON 추출 — EXPLAIN(FORMAT JSON) 결과는 QUERY PLAN 값으로
    # 팅 없는 '[{ ... }]' 형태로 나오지만 SQL 에는 쿼리 벡터 리터럴의 '['도 있으므로
    # 'Execution Time' 키를 직접 라인 기반으로 찾는 것이 가장 안전하다.
    exec_ms = -1.0
    m = re.search(r'"Execution Time"\s*:\s*([0-9.]+)', raw)
    if m:
        try:
            exec_ms = float(m.group(1))
        except ValueError:
            pass

    # Step 2: 실제 결과 조문번호 조회 (동일 ef_search)
    result_sql = f"""
        BEGIN;
        SET LOCAL hnsw.ef_search = {ef};
        SELECT lc.article_no
          FROM legal_chunks lc
         WHERE lc.abolition_date IS NULL
           AND lc.embedding IS NOT NULL
         ORDER BY lc.embedding <=> '{qvec_lit}'::vector
         LIMIT {TOPK};
        ROLLBACK;
    """
    out = run_psql(result_sql)
    articles = []
    for line in out.splitlines():
        line = line.strip()
        if not line or line.startswith("article_no") or line.startswith("-") \
                or line.startswith("(") or line == "BEGIN" or line == "ROLLBACK":
            continue
        articles.append(line)
    return exec_ms, articles


def main():
    print("=" * 90)
    print("Phase B-5 HNSW ef_search 벤치마크")
    print(f"EF 후보: {EF_VALUES}, topK={TOPK}, 반복={REPEAT}회/ef")
    print("=" * 90)

    # 각 쿼리별 ef 벤치마크
    # 전체 요약도 집계
    summary_latency = {ef: [] for ef in EF_VALUES}
    summary_recall_vs_max_ef = {ef: [] for ef in EF_VALUES}

    for q in QUERIES:
        print(f"\n[쿼리] {q}")
        qvec = embed_query(q)
        qvec_lit = to_pgvector_literal(qvec)

        # 기준(가장 큰 ef)의 상위 K를 ground truth로 두고 recall@K 계산
        max_ef = max(EF_VALUES)
        _, gt_articles = benchmark_one(qvec_lit, max_ef)
        gt_set = set(gt_articles)

        for ef in EF_VALUES:
            latencies = []
            articles_last = []
            for _ in range(REPEAT):
                exec_ms, articles = benchmark_one(qvec_lit, ef)
                if exec_ms >= 0:
                    latencies.append(exec_ms)
                articles_last = articles
            median_ms = statistics.median(latencies) if latencies else -1.0
            overlap = len(set(articles_last) & gt_set)
            recall = overlap / len(gt_set) if gt_set else 0.0

            summary_latency[ef].append(median_ms)
            summary_recall_vs_max_ef[ef].append(recall)

            print(f"  ef={ef:4d}  median={median_ms:7.2f}ms  "
                  f"recall@{TOPK}={recall*100:5.1f}%  top3={articles_last[:3]}")

    print("\n" + "=" * 90)
    print("전체 요약 (쿼리 4개 평균)")
    print("=" * 90)
    print(f"{'ef_search':>10s}  {'median lat (ms)':>18s}  {'avg recall vs ef=' + str(max(EF_VALUES)):>28s}")
    for ef in EF_VALUES:
        lat = statistics.median(summary_latency[ef]) if summary_latency[ef] else -1.0
        rec = statistics.mean(summary_recall_vs_max_ef[ef]) if summary_recall_vs_max_ef[ef] else 0.0
        marker = " ← default" if ef == 80 else ""
        print(f"{ef:>10d}  {lat:>15.2f}     {rec*100:>22.1f}%{marker}")


if __name__ == "__main__":
    main()
