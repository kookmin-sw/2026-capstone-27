#!/usr/bin/env python3
"""
R-17 의도 분류 재검증 스크립트 (Cohere v2 / command-a-03-2025).

레거시 LLM(llama-4-scout) 기준 결과(docs/test-results/r17-intent-classification-results.json)를
같은 12케이스 × 동일 프롬프트로 Cohere v2 API에서 재측정한다.

- 입력 쿼리: 기존 결과 파일의 intent_summary 필드 (원본 질의 미보존으로 인해 대용)
- 프롬프트: src/main/resources/ai/prompts/rag/intent-classifier.md
- 온톨로지: src/main/resources/ontology/legal-ontology-slim.json
- 출력: docs/test-results/r17-cohere-rerun-results.json
"""

import json
import os
import sys
import time
from pathlib import Path
from urllib import request as urlrequest
from urllib.error import HTTPError

ROOT = Path(__file__).resolve().parent.parent
PROMPT_PATH = ROOT / "src/main/resources/ai/prompts/rag/intent-classifier.md"
ONTOLOGY_PATH = ROOT / "src/main/resources/ontology/legal-ontology-slim.json"
LEGACY_RESULTS_PATH = ROOT / "docs/test-results/r17-intent-classification-results.json"
OUTPUT_PATH = ROOT / "docs/test-results/r17-cohere-rerun-results.json"

API_KEY = os.environ.get("COHERE_API_KEY")
if not API_KEY:
    print("ERROR: COHERE_API_KEY env var required", file=sys.stderr)
    sys.exit(2)

MODEL = os.environ.get("COHERE_MODEL", "command-a-03-2025")
ENDPOINT = "https://api.cohere.com/v2/chat"

def load_inputs():
    prompt_template = PROMPT_PATH.read_text(encoding="utf-8")
    ontology_json = ONTOLOGY_PATH.read_text(encoding="utf-8")
    legacy = json.loads(LEGACY_RESULTS_PATH.read_text(encoding="utf-8"))
    return prompt_template, ontology_json, legacy["results"]

def build_prompt(template: str, ontology_json: str, user_query: str) -> str:
    # 대화 내역은 USER 단일 turn으로 구성 (레거시 테스트와 동일 조건)
    conversation = f"USER: {user_query}"
    return (template
            .replace("{ONTOLOGY_JSON}", ontology_json)
            .replace("{CONVERSATION_HISTORY}", conversation))

def call_cohere(prompt: str) -> dict:
    body = {
        "model": MODEL,
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0.1,
        "max_tokens": 512,
        "response_format": {"type": "json_object"},
    }
    req = urlrequest.Request(
        ENDPOINT,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {API_KEY}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    t0 = time.time()
    with urlrequest.urlopen(req, timeout=60) as resp:
        raw = resp.read().decode("utf-8")
    dt_ms = int((time.time() - t0) * 1000)
    data = json.loads(raw)
    return data, dt_ms

def extract_text(resp_json: dict) -> str:
    content = resp_json.get("message", {}).get("content", [])
    for block in content:
        if block.get("type") == "text":
            return block.get("text", "")
    return ""

def parse_classify_json(text: str) -> dict:
    text = text.strip()
    # Cohere가 코드펜스 감싸는 경우 제거
    if text.startswith("```"):
        lines = [ln for ln in text.splitlines() if not ln.strip().startswith("```")]
        text = "\n".join(lines).strip()
    return json.loads(text)

def score_case(parsed: dict, expected_l1: str, expected_l2: str):
    nodes = parsed.get("matched_nodes", [])
    if not nodes:
        return None, 0.0, False, False, []
    first = nodes[0]
    actual_id = first.get("id", "")
    conf = float(first.get("confidence", 0.0))
    # L1/L2 매칭: actual_id가 expected로 시작하는지
    l1_match = actual_id.startswith(expected_l1)
    l2_match = actual_id.startswith(expected_l2)
    all_nodes = [(n.get("id"), float(n.get("confidence", 0.0))) for n in nodes]
    return actual_id, conf, l1_match, l2_match, all_nodes

def main():
    prompt_template, ontology_json, legacy_cases = load_inputs()
    results = []
    l1_hits = 0
    l2_hits = 0
    errors = 0
    confs = []
    latencies = []
    total_input_tokens = 0
    total_output_tokens = 0
    total_billed_input = 0
    total_billed_output = 0

    for case in legacy_cases:
        idx = case["idx"]
        domain = case["domain"]
        scenario = case["scenario"]
        expected_l1 = case["expected_l1"]
        expected_l2 = case["expected_l2"]
        query = case["intent_summary"]  # 원본 질의 대용
        legacy_actual = case["actual_id"]
        legacy_conf = case["confidence"]
        legacy_l1 = case["l1_match"]
        legacy_l2 = case["l2_match"]

        prompt = build_prompt(prompt_template, ontology_json, query)
        actual_id = None
        conf = 0.0
        l1_match = False
        l2_match = False
        all_nodes = []
        parsed = None
        err = None
        dt_ms = None
        usage = {}
        try:
            resp, dt_ms = call_cohere(prompt)
            text = extract_text(resp)
            parsed = parse_classify_json(text)
            actual_id, conf, l1_match, l2_match, all_nodes = score_case(parsed, expected_l1, expected_l2)
            usage = resp.get("usage", {})
            billed = usage.get("billed_units", {}) or {}
            tokens = usage.get("tokens", {}) or {}
            total_billed_input += int(billed.get("input_tokens") or 0)
            total_billed_output += int(billed.get("output_tokens") or 0)
            total_input_tokens += int(tokens.get("input_tokens") or 0)
            total_output_tokens += int(tokens.get("output_tokens") or 0)
            latencies.append(dt_ms)
            if l1_match: l1_hits += 1
            if l2_match: l2_hits += 1
            confs.append(conf)
        except HTTPError as e:
            err = f"HTTP {e.code}: {e.read().decode('utf-8', errors='replace')[:200]}"
            errors += 1
        except Exception as e:
            err = f"{type(e).__name__}: {e}"
            errors += 1

        results.append({
            "idx": idx,
            "domain": domain,
            "scenario": scenario,
            "query_used": query,
            "expected_l1": expected_l1,
            "expected_l2": expected_l2,
            "cohere": {
                "actual_id": actual_id,
                "confidence": conf,
                "l1_match": l1_match,
                "l2_match": l2_match,
                "all_nodes": all_nodes,
                "intent_summary": (parsed or {}).get("intent_summary"),
                "latency_ms": dt_ms,
                "usage": usage,
                "error": err,
            },
            "legacy_baseline": {
                "actual_id": legacy_actual,
                "confidence": legacy_conf,
                "l1_match": legacy_l1,
                "l2_match": legacy_l2,
                "model": case.get("model", "llama-4-scout"),
            },
        })
        print(f"[{idx:>2}] {domain}/{scenario}: actual={actual_id} conf={conf} L1={l1_match} L2={l2_match} dt={dt_ms}ms err={err}")
        # 분당 호출 간격 (Cohere Trial rate limit 여유분)
        time.sleep(1.5)

    n = len(results)
    ok = n - errors
    summary = {
        "model": MODEL,
        "total_cases": n,
        "errors": errors,
        "l1_accuracy": f"{l1_hits}/{ok} = {(l1_hits / ok * 100) if ok else 0:.1f}%",
        "l2_accuracy": f"{l2_hits}/{ok} = {(l2_hits / ok * 100) if ok else 0:.1f}%",
        "avg_confidence": round(sum(confs) / len(confs), 3) if confs else None,
        "avg_latency_ms": int(sum(latencies) / len(latencies)) if latencies else None,
        "p95_latency_ms": sorted(latencies)[int(len(latencies) * 0.95) - 1] if latencies else None,
        "total_billed_input_tokens": total_billed_input,
        "total_billed_output_tokens": total_billed_output,
        "total_tokens_input": total_input_tokens,
        "total_tokens_output": total_output_tokens,
    }
    out = {"summary": summary, "results": results}
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print("\n=== SUMMARY ===")
    for k, v in summary.items():
        print(f"  {k}: {v}")
    print(f"\nSaved: {OUTPUT_PATH}")

if __name__ == "__main__":
    main()
