#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
법제처 LOD SPARQL 엔드포인트에서 민법 관련 법률 온톨로지를 배치 다운로드.
POST 방식으로 HTML 테이블을 파싱하여 결과를 수집한 뒤,
민법 관련 법률만 필터링하여 CSV로 저장.
"""

import csv
import json
import re
import sys
import time
import pathlib
import requests

BASE_URL = "https://lod.law.go.kr/DRF/lod/sparql.do"
OUTPUT_DIR = pathlib.Path("lod_law_downloads")
CSV_ALL_PATH = OUTPUT_DIR / "all_legislation.csv"
CSV_CIVIL_PATH = OUTPUT_DIR / "civil_law_legislation.csv"
JSON_CIVIL_PATH = OUTPUT_DIR / "civil_law_legislation.json"

BATCH_SIZE = 500
SLEEP_SEC = 1.0
TIMEOUT = 120

# 민법 관련 키워드 필터
CIVIL_LAW_KEYWORDS = [
    "민법", "민사", "부동산", "임대차", "매매", "상속", "유언", "유류분",
    "이혼", "위자료", "재산분할", "양육", "친권", "후견", "가사",
    "채권", "채무", "보증", "담보", "저당", "근저당", "경매",
    "손해배상", "불법행위", "명예훼손", "초상권", "개인정보",
    "계약", "소유권", "점유", "물권", "용익", "지상권", "지역권",
    "소멸시효", "파산", "회생", "면책", "가압류", "가처분",
    "임차", "전세", "보증금", "권리금", "명도",
]

QUERY_TEMPLATE = """PREFIX ldc: <http://lod.law.go.kr/Class/>
PREFIX ldp: <http://lod.law.go.kr/property/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?s ?name ?lawCode ?announceDate ?enforceDate WHERE {{
  ?s a ldc:KoreanLegislation .
  ?s ldp:lawName ?name .
  OPTIONAL {{ ?s ldp:lawCode ?lawCode }}
  OPTIONAL {{ ?s ldp:announceDate ?announceDate }}
  OPTIONAL {{ ?s ldp:enforceDate ?enforceDate }}
}} LIMIT {limit} OFFSET {offset}"""


def fetch_batch(session: requests.Session, offset: int, limit: int) -> str:
    query = QUERY_TEMPLATE.format(limit=limit, offset=offset)
    resp = session.post(
        BASE_URL,
        data={"query": query, "time_out": "60"},
        headers={"User-Agent": "Mozilla/5.0 (compatible; ClaudeCode-LegalOntologyDownloader/1.0)"},
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    return resp.text


def parse_html_table(html: str) -> list[dict]:
    """HTML 테이블에서 SPARQL 결과 행을 파싱."""
    idx = html.find('<tbody class="_result">')
    if idx < 0:
        return []
    end = html.find("</tbody>", idx)
    tbody = html[idx : end + 8]

    rows = re.findall(r'<tr id="tb_tr">(.*?)</tr>', tbody, re.DOTALL)
    results = []
    for row in rows:
        tds = re.findall(r"<td[^>]*>(.*?)</td>", row, re.DOTALL)
        cells = []
        for td in tds:
            cell = re.sub(r"<[^>]+>", "", td).strip()
            cell = cell.replace("&#034;", '"').replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            # Remove language tags like "@ko" and datatype tags
            cell = re.sub(r'"@\w+$', "", cell)
            cell = re.sub(r'\^\^.*$', "", cell)
            cell = cell.strip('"')
            cells.append(cell)

        if len(cells) >= 2:
            row_dict = {
                "resource_uri": cells[0] if len(cells) > 0 else "",
                "law_name": cells[1] if len(cells) > 1 else "",
                "law_code": cells[2] if len(cells) > 2 else "",
                "announce_date": cells[3] if len(cells) > 3 else "",
                "enforce_date": cells[4] if len(cells) > 4 else "",
            }
            results.append(row_dict)

    return results


def is_civil_law(name: str) -> bool:
    """법률명이 민법 관련 키워드를 포함하는지 확인."""
    return any(kw in name for kw in CIVIL_LAW_KEYWORDS)


def main():
    if sys.platform == "win32":
        sys.stdout.reconfigure(encoding="utf-8")

    OUTPUT_DIR.mkdir(exist_ok=True)
    session = requests.Session()

    all_rows = []
    batch_no = 0

    while True:
        offset = batch_no * BATCH_SIZE
        print(f"[INFO] batch={batch_no} offset={offset} limit={BATCH_SIZE}")

        try:
            html = fetch_batch(session, offset=offset, limit=BATCH_SIZE)
        except requests.RequestException as e:
            print(f"[ERROR] request failed at batch {batch_no}: {e}")
            break

        rows = parse_html_table(html)
        print(f"[INFO] parsed {len(rows)} rows")

        if not rows:
            print("[INFO] no more results, stopping")
            break

        all_rows.extend(rows)
        batch_no += 1

        if len(rows) < BATCH_SIZE:
            print("[INFO] last batch (partial), stopping")
            break

        time.sleep(SLEEP_SEC)

    print(f"\n[INFO] total legislation downloaded: {len(all_rows)}")

    # Save all legislation CSV
    fieldnames = ["resource_uri", "law_name", "law_code", "announce_date", "enforce_date"]
    with CSV_ALL_PATH.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in all_rows:
            writer.writerow(row)
    print(f"[DONE] all legislation CSV: {CSV_ALL_PATH.resolve()}")

    # Filter civil law related
    civil_rows = [r for r in all_rows if is_civil_law(r["law_name"])]
    print(f"[INFO] civil law related: {len(civil_rows)} / {len(all_rows)}")

    with CSV_CIVIL_PATH.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in civil_rows:
            writer.writerow(row)
    print(f"[DONE] civil law CSV: {CSV_CIVIL_PATH.resolve()}")

    # Save JSON too
    with JSON_CIVIL_PATH.open("w", encoding="utf-8") as f:
        json.dump(civil_rows, f, ensure_ascii=False, indent=2)
    print(f"[DONE] civil law JSON: {JSON_CIVIL_PATH.resolve()}")


if __name__ == "__main__":
    main()
