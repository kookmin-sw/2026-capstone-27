#!/usr/bin/env python3
"""Phase B-4 3-way 하이브리드 검색 검증 스크립트.

Cohere embed API로 쿼리 임베딩을 생성하고, Spring 서비스의 네이티브 쿼리와
동일한 SQL을 psql로 실행해 상위 K 결과를 출력한다.

사용법:
  COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/verify_3way_search.py
"""
import json
import os
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
WV, WK, WT = 0.5, 0.3, 0.2  # application.yml 기본값과 동일
TOPK = 5

TEST_CASES = [
    {
        "label": "전세 보증금 미반환 (R-17 #1 회귀 케이스)",
        "vector_query": "전세 계약 종료 후 집주인이 보증금을 돌려주지 않음",
        "bm25_keywords": ["전세권", "전세금", "보증금", "반환"],
        "category_ids": ["group:jeonse"],
        "expected_articles": ["제303조", "제312조", "제313조", "제317조", "제318조"],
    },
    {
        "label": "임대차 계약 해지",
        "vector_query": "임차인이 차임을 계속 연체하여 임대차 계약을 해지하고 싶음",
        "bm25_keywords": ["임대차", "차임", "연체", "해지"],
        "category_ids": ["group:leasing"],
        "expected_articles": ["제640조", "제635조", "제618조"],
    },
    {
        "label": "저당권 실행",
        "vector_query": "채무자가 돈을 갚지 않아 담보로 잡은 부동산에 설정한 저당권을 실행하려 함",
        "bm25_keywords": ["저당권", "저당물", "실행", "경매"],
        "category_ids": ["group:mortgage"],
        "expected_articles": ["제356조", "제363조"],
    },
    {
        "label": "소유권 이전과 등기",
        "vector_query": "부동산을 매수했는데 소유권이전등기를 하지 않으면 어떻게 되는가",
        "bm25_keywords": ["소유권", "이전", "등기"],
        "category_ids": None,  # 카테고리 필터 없이
        "expected_articles": ["제186조", "제187조", "제188조"],
    },
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


def build_keyword_query(keywords: list[str]) -> str:
    return " | ".join(keywords)


def run_search(case: dict) -> list[dict]:
    qvec = embed_query(case["vector_query"])
    qvec_lit = to_pgvector_literal(qvec)
    kq = build_keyword_query(case["bm25_keywords"])
    categories = case.get("category_ids")

    # category_ids 파라미터: NULL이면 필터 없이, 아니면 배열 리터럴
    if categories:
        cat_literal = "ARRAY[" + ",".join(f"'{c}'" for c in categories) + "]::text[]"
    else:
        cat_literal = "NULL::text[]"

    sql = f"""
        SELECT lc.law_name,
               lc.article_no,
               lc.article_title,
               ROUND( (
                   CASE WHEN lc.embedding IS NULL THEN 0
                        ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                   END * {WV}
                 + ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1) * {WK}
                 + similarity(lc.content, $${case["vector_query"]}$$) * {WT}
               )::numeric, 4) AS score,
               ROUND((1 - (lc.embedding <=> '{qvec_lit}'::vector))::numeric, 4) AS vec_sim,
               ROUND(ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1)::numeric, 4) AS bm25,
               ROUND(similarity(lc.content, $${case["vector_query"]}$$)::numeric, 4) AS trig
          FROM legal_chunks lc
         WHERE lc.abolition_date IS NULL
           AND lc.law_id = 'law-civil'
           AND ( COALESCE(CARDINALITY({cat_literal}), 0) = 0
                 OR lc.category_ids && {cat_literal} )
           AND ( lc.content_tsv @@ plainto_tsquery('simple', $${case["vector_query"]}$$)
              OR lc.content_tsv @@ to_tsquery('simple', '{kq}')
              OR lc.content % $${case["vector_query"]}$$
              OR lc.embedding IS NOT NULL )
         ORDER BY score DESC
         LIMIT {TOPK};
    """

    env = os.environ.copy()
    env["PGPASSWORD"] = DB_PASS
    conn = f"host={DB_HOST} port=5432 user={DB_USER} dbname={DB_NAME} sslmode=require"
    result = subprocess.run(
        ["psql", conn, "-F", "|", "-A", "-t", "-c", sql],
        env=env,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        print("SQL ERROR:", result.stderr, file=sys.stderr)
        return []
    rows = []
    for line in result.stdout.strip().split("\n"):
        if not line.strip():
            continue
        parts = line.split("|")
        if len(parts) >= 7:
            rows.append({
                "law": parts[0],
                "article_no": parts[1],
                "title": parts[2],
                "score": parts[3],
                "vec_sim": parts[4],
                "bm25": parts[5],
                "trig": parts[6],
            })
    return rows


def main():
    print("=" * 80)
    print("Phase B-4 3-way 하이브리드 검색 검증")
    print(f"weights: vector={WV}, keyword={WK}, trigram={WT}, topK={TOPK}")
    print("=" * 80)

    all_pass = True
    for case in TEST_CASES:
        print(f"\n[{case['label']}]")
        print(f"쿼리      : {case['vector_query']}")
        print(f"키워드    : {case['bm25_keywords']}")
        print(f"카테고리  : {case['category_ids']}")
        print(f"기대 조문 : {case['expected_articles']}")
        rows = run_search(case)
        print(f"결과 (상위 {TOPK}):")
        for i, r in enumerate(rows, 1):
            print(f"  {i}. {r['article_no']:12s} {r['title']:30s}  "
                  f"score={r['score']}  vec={r['vec_sim']}  bm25={r['bm25']}  trig={r['trig']}")
        actuals = [r["article_no"] for r in rows]
        matched = [e for e in case["expected_articles"] if e in actuals]
        print(f"매칭: {len(matched)}/{len(case['expected_articles'])} — {matched}")
        if not matched:
            print("  WARN: 기대 조문이 상위에 없음")
            all_pass = False
    print("\n" + "=" * 80)
    print("전체 통과" if all_pass else "일부 케이스 미매칭 — 가중치 재튜닝 검토 필요")
    print("=" * 80)
    sys.exit(0 if all_pass else 1)


if __name__ == "__main__":
    main()
