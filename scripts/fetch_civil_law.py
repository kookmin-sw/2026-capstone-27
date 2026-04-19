#!/usr/bin/env python3
"""
국가법령정보 OpenAPI에서 민법 전문을 받아와 시드 JSON으로 저장.

출력: src/main/resources/seed/civil-law.json
  - 실제 조문만 (조문여부="조문") 1,193개 추출
  - 본문 + 항/호 병합한 정규화된 텍스트 생성
  - 편/장 헤더(전문)는 book/chapter/section 메타데이터로 재귀 추적하여
    각 조문에 함께 달아둠
  - categories 필드는 이 단계에서는 비워둠 (B-2에서 매핑 테이블 적용)

재실행 멱등. 법령 개정이 있으면 MST 파라미터 혹은 최신 조회로 갱신.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import urlencode

OC = os.environ.get("LAW_OC") or (sys.argv[1] if len(sys.argv) > 1 else None)
if not OC:
    print("Usage: LAW_OC=<key> python3 scripts/fetch_civil_law.py", file=sys.stderr)
    sys.exit(2)

# 민법
LAW_ID = "001706"
LAW_MST = "284415"  # 2026-03-17 시행 현행판. 필요시 lawSearch로 최신 MST 확인

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/seed/civil-law.json"

# 법제처 서버는 TLS 1.2만 허용하고 Python urllib와 SSL 핸드셰이크 상성이 좋지 않아
# curl --tls-max 1.2를 서브프로스로 호출

def fetch_full_law() -> dict:
    params = urlencode({
        "OC": OC,
        "target": "law",
        "MST": LAW_MST,
        "type": "JSON",
    })
    url = f"https://www.law.go.kr/DRF/lawService.do?{params}"
    print(f"[fetch] {url}")
    # 처음 1회에는 서버가 가끔 SSL 핸드셰이크를 끊는 경우가 있어 재시도
    last_err = None
    for attempt in range(3):
        proc = subprocess.run(
            ["curl", "-sS", "--tls-max", "1.2", "--max-time", "60", url],
            capture_output=True, text=True,
        )
        if proc.returncode == 0 and proc.stdout.strip():
            return json.loads(proc.stdout)
        last_err = f"rc={proc.returncode} stderr={proc.stderr.strip()[:200]}"
        print(f"[warn] attempt {attempt+1} failed: {last_err}")
        time.sleep(2)
    raise RuntimeError(f"law.go.kr 요청 실패: {last_err}")


def _as_list(v):
    """법제처 API는 항/호/목 원소가 1개일 때 list로 감싸지 않고 dict를 그대로 넣는다.
    일관된 list로 정규화."""
    if v is None:
        return []
    if isinstance(v, list):
        return v
    return [v]


def render_text(unit: dict) -> str:
    """조문내용 + 각 항/호/목 본문을 연결한 정규화 텍스트 생성."""
    parts: list[str] = []
    head = (unit.get("조문내용") or "").strip()
    if head:
        parts.append(head)

    for hang in _as_list(unit.get("항")):
        if not isinstance(hang, dict):
            continue
        hcontent = (hang.get("항내용") or "").strip()
        if hcontent:
            parts.append(hcontent)
        for ho in _as_list(hang.get("호")):
            if not isinstance(ho, dict):
                continue
            ho_content = (ho.get("호내용") or "").strip()
            if ho_content:
                parts.append(ho_content)
            for mok in _as_list(ho.get("목")):
                if not isinstance(mok, dict):
                    continue
                mok_content = (mok.get("목내용") or "").strip()
                if mok_content:
                    parts.append(mok_content)

    return "\n".join(parts)


def parse_header(전문_content: str) -> tuple[str | None, str | None, str | None]:
    """'제1편 총칙' / '제3장 법률행위' / '제1절 총칙' 등 분석.

    반환: (book, chapter, section) — 해당 레벨만 채우고 나머지는 None.
    """
    s = 전문_content.strip()
    if not s:
        return (None, None, None)
    if "편" in s.split()[0]:
        return (s, None, None)
    if "장" in s.split()[0]:
        return (None, s, None)
    if "절" in s.split()[0]:
        return (None, None, s)
    return (None, None, None)


def extract_articles(law_json: dict) -> list[dict]:
    units = law_json["법령"]["조문"]["조문단위"]
    current_book: str | None = None
    current_chapter: str | None = None
    current_section: str | None = None

    out: list[dict] = []
    for unit in units:
        if unit.get("조문여부") == "전문":
            book, chapter, section = parse_header(unit.get("조문내용") or "")
            if book:
                current_book = book
                current_chapter = None
                current_section = None
            elif chapter:
                current_chapter = chapter
                current_section = None
            elif section:
                current_section = section
            continue

        if unit.get("조문여부") != "조문":
            continue

        try:
            article_no = int(unit["조문번호"])
        except (KeyError, TypeError, ValueError):
            continue

        text = render_text(unit)
        if not text:
            continue

        out.append({
            "law_id": "law-civil",       # SHIELD 내부 ID (온톨로지/매핑 키와 맞출 것)
            "law_name": "민법",
            "article_no": f"제{article_no}조",
            "article_no_int": article_no,
            "article_title": unit.get("조문제목"),
            "content": text,
            "book": current_book,
            "chapter": current_chapter,
            "section": current_section,
            "effective_date": unit.get("조문시행일자"),
            "source_mst": LAW_MST,
            "source_law_id": LAW_ID,
        })
    return out


def main():
    t0 = time.time()
    law_json = fetch_full_law()
    articles = extract_articles(law_json)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "meta": {
            "law_name": "민법",
            "law_id": LAW_ID,
            "mst": LAW_MST,
            "fetched_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "total_articles": len(articles),
            "source": "https://www.law.go.kr/DRF/lawService.do",
        },
        "articles": articles,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    size_kb = OUT.stat().st_size // 1024
    print(f"[ok] {len(articles)}개 조문 → {OUT} ({size_kb} KB, {time.time()-t0:.1f}s)")


if __name__ == "__main__":
    main()
