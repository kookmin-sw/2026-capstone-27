#!/usr/bin/env python3
"""Phase B-8 종료 시점 RAG 정확도 정성 스모크.

10개 대표 질의에 대해 Spring 서비스의 PgLegalRetrievalService와 동일한
3-way 하이브리드 SQL을 psql로 실행해 상위 5개 청크를 Markdown 보고서로 출력한다.

B-8c 변경(BM25 prefix 매칭)과 B-8a 변경(categoryIds soft-filter)을 반영.

사용법:
  COHERE_API_KEY=... DB_PASSWORD=... python3 scripts/rag_qualitative_smoke.py \
      > docs/rag-smoke-report.md
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
WV, WK, WT = 0.5, 0.3, 0.2
TOPK = 5

# 민법 커버리지를 고려해 선정한 10개 대표 질의.
# expected_articles는 "이 조문이 상위에 나오면 건전"이라는 약한 기대치.
TEST_CASES = [
    {
        "label": "Q1 전세 보증금 미반환",
        "vector_query": "전세 계약이 끝났는데 집주인이 보증금을 돌려주지 않음",
        "bm25_keywords": ["전세권", "전세금", "보증금", "반환"],
        "category_ids": ["group:jeonse"],
        "expected_articles": ["제303조", "제312조의2", "제317조", "제318조"],
    },
    {
        "label": "Q2 임차인 차임 연체로 임대차 해지",
        "vector_query": "임차인이 차임을 2기 이상 연체하여 임대차 계약을 해지하고자 함",
        "bm25_keywords": ["임대차", "차임", "연체", "해지"],
        "category_ids": ["group:leasing"],
        "expected_articles": ["제640조", "제635조", "제618조"],
    },
    {
        "label": "Q3 저당권 실행과 경매",
        "vector_query": "채무자가 돈을 갚지 않아 담보 부동산에 설정한 저당권을 실행하려 함",
        "bm25_keywords": ["저당권", "저당물", "경매", "실행"],
        "category_ids": ["group:mortgage"],
        "expected_articles": ["제356조", "제363조"],
    },
    {
        "label": "Q4 소유권 이전 등기 미이행",
        "vector_query": "부동산을 매수했는데 소유권이전등기를 하지 않으면 어떻게 되는가",
        "bm25_keywords": ["소유권", "이전", "등기"],
        "category_ids": None,
        "expected_articles": ["제186조", "제187조", "제188조"],
    },
    {
        "label": "Q5 매매 하자담보책임",
        "vector_query": "산 물건에 하자가 있어서 매도인에게 책임을 묻고 싶음",
        "bm25_keywords": ["매매", "하자", "담보책임"],
        "category_ids": None,
        "expected_articles": ["제580조", "제581조", "제582조", "제575조"],
    },
    {
        "label": "Q6 상속 한정승인",
        "vector_query": "부모가 돌아가셨는데 빚이 많아 한정승인을 하고 싶음",
        "bm25_keywords": ["상속", "한정승인", "상속채무"],
        "category_ids": None,
        "expected_articles": ["제1028조", "제1030조", "제1019조"],
    },
    {
        "label": "Q7 유류분 반환청구",
        "vector_query": "아버지가 형에게 재산을 전부 주셔서 유류분을 반환 청구하려 함",
        "bm25_keywords": ["유류분", "반환청구"],
        "category_ids": None,
        "expected_articles": ["제1112조", "제1113조", "제1115조"],
    },
    {
        "label": "Q8 불법행위 손해배상",
        "vector_query": "타인의 고의 또는 과실로 손해를 입어 배상을 청구하려 함",
        "bm25_keywords": ["불법행위", "손해배상", "고의", "과실"],
        "category_ids": None,
        "expected_articles": ["제750조", "제751조", "제763조"],
    },
    {
        "label": "Q9 금전채무 소멸시효",
        "vector_query": "빌려준 돈의 소멸시효 기간이 얼마나 되는가",
        "bm25_keywords": ["소멸시효", "채권"],
        "category_ids": None,
        "expected_articles": ["제162조", "제163조", "제174조"],
    },
    {
        "label": "Q10 보증인 구상권",
        "vector_query": "보증인이 대신 변제한 후 주채무자에게 구상할 수 있는지",
        "bm25_keywords": ["보증", "구상권", "변제"],
        "category_ids": ["group:guaranty_debtors"],
        "expected_articles": ["제441조", "제442조", "제444조"],
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


def to_pgvector_literal(vec):
    return "[" + ",".join(f"{v:.6f}" for v in vec) + "]"


def with_prefix(token: str) -> str:
    """Java PgLegalRetrievalService.withPrefixMatch와 동일 규칙."""
    return token + ":*" if len(token) >= 2 else token


def build_keyword_query(keywords):
    return " | ".join(with_prefix(k) for k in keywords)


def run_search(case):
    qvec = embed_query(case["vector_query"])
    qvec_lit = to_pgvector_literal(qvec)
    kq = build_keyword_query(case["bm25_keywords"])
    categories = case.get("category_ids")

    if categories:
        cat_literal = "ARRAY[" + ",".join(f"'{c}'" for c in categories) + "]::text[]"
    else:
        cat_literal = "NULL::text[]"

    # Spring 서비스 SQL과 동일 — BM25는 to_tsquery, 벡터는 plainto_tsquery 경로도 공존
    sql = f"""
        SELECT lc.law_name || '|' || lc.article_no || '|' ||
               COALESCE(lc.article_title,'') || '|' ||
               ROUND( (
                   CASE WHEN lc.embedding IS NULL THEN 0
                        ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                   END * {WV}
                 + ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1) * {WK}
                 + similarity(lc.content, $${case["vector_query"]}$$) * {WT}
               )::numeric, 4) || '|' ||
               ROUND((1 - (lc.embedding <=> '{qvec_lit}'::vector))::numeric, 4) || '|' ||
               ROUND(ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1)::numeric, 4) || '|' ||
               ROUND(similarity(lc.content, $${case["vector_query"]}$$)::numeric, 4)
          FROM legal_chunks lc
         WHERE lc.abolition_date IS NULL
           AND lc.law_id = 'law-civil'
           AND ( COALESCE(CARDINALITY({cat_literal}), 0) = 0
                 OR lc.category_ids && {cat_literal} )
           AND ( lc.content_tsv @@ plainto_tsquery('simple', $${case["vector_query"]}$$)
              OR lc.content_tsv @@ to_tsquery('simple', '{kq}')
              OR lc.content % $${case["vector_query"]}$$
              OR lc.embedding IS NOT NULL )
         ORDER BY (
                   CASE WHEN lc.embedding IS NULL THEN 0
                        ELSE 1 - (lc.embedding <=> '{qvec_lit}'::vector)
                   END * {WV}
                 + ts_rank(lc.content_tsv, to_tsquery('simple', '{kq}'), 1) * {WK}
                 + similarity(lc.content, $${case["vector_query"]}$$) * {WT}
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
        print("SQL ERROR:", result.stderr, file=sys.stderr)
        return [], kq
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
    return rows, kq


def main():
    print("# RAG 정확도 정성 스모크 보고서")
    print()
    print("작성: Phase B-8 종료 시점 (B-8a/B-8b/B-8c 반영)")
    print(f"- 가중치: vector={WV}, keyword(BM25)={WK}, trigram={WT}")
    print(f"- topK: {TOPK}")
    print("- BM25 쿼리: prefix 매칭 (`키워드:*`) 적용")
    print("- 코퍼스: 민법 1,193 chunk (law-civil)")
    print()
    print("**판정 기준**: 상위 5개 중 `expected_articles`가 1개 이상 포함되면 관련성 OK로 본다 (정성 평가).")
    print()

    hit_total = 0
    for case in TEST_CASES:
        print(f"## {case['label']}")
        print()
        print(f"- 질의: {case['vector_query']}")
        print(f"- BM25 키워드: {case['bm25_keywords']}")
        print(f"- 카테고리 필터: {case['category_ids']}")
        print(f"- 기대 조문 (관련): {case['expected_articles']}")
        rows, kq = run_search(case)
        print(f"- BM25 tsquery 실제값: `{kq}`")
        print()
        print("| # | 조문 | 제목 | score | vec | bm25 | trig |")
        print("|---|---|---|---|---|---|---|")
        for i, r in enumerate(rows, 1):
            title = (r["title"] or "").replace("|", "/")[:30]
            print(f"| {i} | {r['article_no']} | {title} | {r['score']} | {r['vec_sim']} | {r['bm25']} | {r['trig']} |")
        actuals = [r["article_no"] for r in rows]
        matched = [e for e in case["expected_articles"] if e in actuals]
        verdict = "OK" if matched else "MISS"
        if matched:
            hit_total += 1
        print()
        print(f"- 매칭: {len(matched)}/{len(case['expected_articles'])} → {matched} — **{verdict}**")
        print()

    print("---")
    print()
    print(f"## 종합")
    print()
    print(f"- 관련 조문 상위 노출 성공: **{hit_total}/{len(TEST_CASES)}** 케이스")


if __name__ == "__main__":
    main()
