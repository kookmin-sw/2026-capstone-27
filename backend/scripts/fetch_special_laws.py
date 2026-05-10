#!/usr/bin/env python3
"""
국가법령정보 OpenAPI에서 특별법 18개 전문을 받아와 각 법령별 시드 JSON으로 저장.

출력: src/main/resources/seed/special-laws/<law_id>.json
  - 각 법령마다 CivilLawSeed와 동일 스키마 (Java DTO 재사용)
  - 실제 조문만 추출 (조문여부="조문")
  - 본문 + 항/호/목 병합한 정규화된 텍스트 생성
  - 편/장/절 헤더 재귀 추적
  - categories 는 law_id 기반으로 후속 단계(SpecialLawCategoryMap)에서 주입

재실행 멱등. 법제처 API 일시 실패 시 해당 법령만 스킵하고 나머지는 계속 진행.

실행:
    LAW_OC=<email_id> python3 scripts/fetch_special_laws.py
    LAW_OC=<email_id> python3 scripts/fetch_special_laws.py law-housing-lease   # 개별
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import urlencode

OC = os.environ.get("LAW_OC") or (sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("law-") else None)
if not OC:
    print("Usage: LAW_OC=<key> python3 scripts/fetch_special_laws.py [law_id]", file=sys.stderr)
    sys.exit(2)

# 명령행 2번째 인자 또는 3번째(OC를 인자로 준 경우) = 특정 law_id 단일 실행
TARGET_LAW_ID: str | None = None
for arg in sys.argv[1:]:
    if arg.startswith("law-"):
        TARGET_LAW_ID = arg
        break

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "src/main/resources/seed/special-laws"
OUT_DIR.mkdir(parents=True, exist_ok=True)

# ------------------------------------------------------------------
# 대상 법령 목록 (18개, 민법 LSI265307 제외)
# ------------------------------------------------------------------
# law_id: 내부 식별자 (SHIELD 규약)
# law_name: 공식 법령명
# lsi: 법제처 LOD 법령ID = DRF API의 ID 파라미터와 동일
# ------------------------------------------------------------------
SPECIAL_LAWS: list[dict] = [
    {"law_id": "law-housing-lease",        "law_name": "주택임대차보호법",                                                "lsi": "249999"},
    {"law_id": "law-housing-lease-enf",    "law_name": "주택임대차보호법 시행령",                                           "lsi": "267649"},
    {"law_id": "law-commercial-lease",     "law_name": "상가건물 임대차보호법",                                             "lsi": "238797"},
    {"law_id": "law-commercial-lease-enf", "law_name": "상가건물 임대차보호법 시행령",                                         "lsi": "267689"},
    {"law_id": "law-real-estate-title",    "law_name": "부동산 실권리자명의 등기에 관한 법률",                                    "lsi": "215759"},
    {"law_id": "law-real-estate-reg",      "law_name": "부동산등기법",                                                   "lsi": "265377"},
    {"law_id": "law-family-proc",          "law_name": "가사소송법",                                                     "lsi": "249997"},
    {"law_id": "law-child-support",        "law_name": "양육비 이행확보 및 지원에 관한 법률",                                    "lsi": "276893"},
    {"law_id": "law-inheritance-tax",      "law_name": "상속세 및 증여세법",                                                "lsi": "276123"},
    {"law_id": "law-wage-guarantee",       "law_name": "임금채권보장법",                                                   "lsi": "259881"},
    {"law_id": "law-surety-protect",       "law_name": "보증인 보호를 위한 특별법",                                           "lsi": "251943"},
    {"law_id": "law-civil-exec",           "law_name": "민사집행법",                                                     "lsi": "265351"},
    {"law_id": "law-lease-reg-rule",       "law_name": "임차권등기명령 절차에 관한 규칙",                                      "lsi": "252747"},
    {"law_id": "law-debtor-rehab",         "law_name": "채무자 회생 및 파산에 관한 법률",                                      "lsi": "267359"},
    {"law_id": "law-debt-collect",         "law_name": "채권의 공정한 추심에 관한 법률",                                      "lsi": "268669"},
    {"law_id": "law-privacy",              "law_name": "개인정보 보호법",                                                 "lsi": "270351"},
    {"law_id": "law-fraud-lease-victim",   "law_name": "전세사기피해자 지원 및 주거안정에 관한 특별법",                              "lsi": "271123"},
    {"law_id": "law-auto-accident",        "law_name": "자동차손해배상 보장법",                                              "lsi": "277017"},
]


def _curl_json(url: str) -> dict:
    """법제처 서버로 GET → JSON 파싱. 3회 재시도."""
    last_err = None
    for attempt in range(3):
        proc = subprocess.run(
            ["curl", "-sS", "--tls-max", "1.2", "--max-time", "60", url],
            capture_output=True, text=True,
        )
        if proc.returncode == 0 and proc.stdout.strip():
            try:
                return json.loads(proc.stdout)
            except json.JSONDecodeError as e:
                last_err = f"JSON decode error: {e}; body[:200]={proc.stdout[:200]}"
        else:
            last_err = f"rc={proc.returncode} stderr={proc.stderr.strip()[:200]}"
        print(f"  [warn] attempt {attempt+1} failed: {last_err}")
        time.sleep(2)
    raise RuntimeError(f"law.go.kr 요청 실패: {last_err}")


def resolve_mst_by_name(law_name: str, kind: str | None = None) -> tuple[str, str]:
    """법령명 → (MST, 법령ID). kind 가 주어지면 법령구분명 일치 항목 우선.

    법제처 LOD의 LSI는 특정 개정판 식별자라 현행 MST와 다를 수 있으므로
    lawSearch로 현행판 MST를 먼저 얻은 뒤 lawService를 호출한다.
    """
    params = urlencode({
        "OC": OC,
        "target": "law",
        "query": law_name,
        "type": "JSON",
        "display": "20",
    })
    url = f"https://www.law.go.kr/DRF/lawSearch.do?{params}"
    data = _curl_json(url)

    laws = data.get("LawSearch", {}).get("law", [])
    if isinstance(laws, dict):
        laws = [laws]
    if not laws:
        raise RuntimeError(f"법령 검색 결과 없음: {law_name}")

    # 정확히 일치하는 법령명 우선
    exact = [l for l in laws if l.get("법령명한글", "").strip() == law_name.strip()]
    candidates = exact or laws

    # 현행연혁코드=현행 우선
    current = [l for l in candidates if l.get("현행연혁코드") == "현행"]
    if current:
        candidates = current

    # 법령구분(법률/대통령령/규칙) 필터
    if kind:
        by_kind = [l for l in candidates if l.get("법령구분명") == kind]
        if by_kind:
            candidates = by_kind

    # 시행일자 최신 순
    candidates.sort(key=lambda l: l.get("시행일자", ""), reverse=True)
    top = candidates[0]
    return top["법령일련번호"], top["법령ID"]


def fetch_full_law(mst: str, law_name: str) -> dict:
    """MST로 현행 법령 전문 조회."""
    params = urlencode({
        "OC": OC,
        "target": "law",
        "MST": mst,
        "type": "JSON",
    })
    url = f"https://www.law.go.kr/DRF/lawService.do?{params}"
    print(f"[fetch] {law_name} MST={mst}")
    return _curl_json(url)


def _as_list(v):
    if v is None:
        return []
    if isinstance(v, list):
        return v
    return [v]


def _str_content(raw) -> str:
    """조문내용/항내용/호내용/목내용이 문자열 또는 문자열 list로 올 수 있음."""
    if raw is None:
        return ""
    if isinstance(raw, str):
        return raw.strip()
    if isinstance(raw, list):
        return "\n".join(str(x).strip() for x in raw if x is not None and str(x).strip())
    return str(raw).strip()


def render_text(unit: dict) -> str:
    parts: list[str] = []
    head = _str_content(unit.get("조문내용"))
    if head:
        parts.append(head)
    for hang in _as_list(unit.get("항")):
        if not isinstance(hang, dict):
            continue
        hcontent = _str_content(hang.get("항내용"))
        if hcontent:
            parts.append(hcontent)
        for ho in _as_list(hang.get("호")):
            if not isinstance(ho, dict):
                continue
            ho_content = _str_content(ho.get("호내용"))
            if ho_content:
                parts.append(ho_content)
            for mok in _as_list(ho.get("목")):
                if not isinstance(mok, dict):
                    continue
                mok_content = _str_content(mok.get("목내용"))
                if mok_content:
                    parts.append(mok_content)
    return "\n".join(parts)


def parse_header(전문_content: str) -> tuple[str | None, str | None, str | None]:
    s = 전문_content.strip()
    if not s:
        return (None, None, None)
    first = s.split()[0] if s.split() else ""
    if "편" in first:
        return (s, None, None)
    if "장" in first:
        return (None, s, None)
    if "절" in first:
        return (None, None, s)
    return (None, None, None)


def extract_articles(law_json: dict, law_id: str, law_name: str,
                     current_law_id: str, mst: str, lod_lsi: str) -> list[dict]:
    """법제처 응답 → Article 리스트."""
    try:
        units_raw = law_json["법령"]["조문"]["조문단위"]
    except (KeyError, TypeError):
        print(f"  [warn] 조문단위 없음: {law_name}")
        return []
    units = _as_list(units_raw)

    current_book: str | None = None
    current_chapter: str | None = None
    current_section: str | None = None

    out: list[dict] = []
    for unit in units:
        if not isinstance(unit, dict):
            continue
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

        # 가지조문(예: 제3조의2) 처리 - 조문가지번호
        branch = unit.get("조문가지번호")
        if branch and str(branch) not in ("0", ""):
            article_no_str = f"제{article_no}조의{branch}"
        else:
            article_no_str = f"제{article_no}조"

        text = render_text(unit)
        if not text:
            continue

        out.append({
            "law_id": law_id,
            "law_name": law_name,
            "article_no": article_no_str,
            "article_no_int": article_no,
            "article_title": unit.get("조문제목"),
            "content": text,
            "book": current_book,
            "chapter": current_chapter,
            "section": current_section,
            "effective_date": unit.get("조문시행일자"),
            "source_mst": str(mst),
            # 인제스트 시 CategoryLawMappingService.resolveCategoriesByLsi()가
            # 이 값으로 역조회하므로, 온톨로지에 등록된 LOD LSI를 유지한다.
            "source_law_id": lod_lsi,
        })
    return out


# 법령구분명 힌트 (lawSearch 필터용)
LAW_KIND_HINT: dict[str, str] = {
    "law-housing-lease-enf": "대통령령",
    "law-commercial-lease-enf": "대통령령",
    "law-lease-reg-rule": "대법원규칙",
}


def process_law(entry: dict) -> dict:
    """1개 법령 처리 → seed JSON 파일 생성 → 요약 dict 반환."""
    law_id = entry["law_id"]
    law_name = entry["law_name"]
    lsi = entry["lsi"]
    kind_hint = LAW_KIND_HINT.get(law_id)

    t0 = time.time()
    try:
        mst, current_law_id = resolve_mst_by_name(law_name, kind_hint)
        print(f"  [resolve] {law_name} → MST={mst}, 법령ID={current_law_id} (LOD LSI={lsi})")
        law_json = fetch_full_law(mst, law_name)
    except Exception as e:
        print(f"  [error] fetch 실패: {e}")
        return {"law_id": law_id, "ok": False, "error": str(e)[:200]}

    # 현행판 MST로 교체된 값으로 seed 파일에 기록 (source_law_id는 현행 법령ID,
    # source_mst는 현행 MST). 온톨로지 역인덱스는 여전히 LOD LSI로 조회하므로
    # 별도 lod_lsi 필드로 보존.
    articles = extract_articles(law_json, law_id, law_name, current_law_id, mst, lsi)
    if not articles:
        return {"law_id": law_id, "ok": False, "error": "no articles extracted"}

    out_path = OUT_DIR / f"{law_id}.json"
    # articles 에 들어간 값에서 MST/현행 법령ID 가져오기
    mst = articles[0]["source_mst"]
    payload = {
        "meta": {
            "law_name": law_name,
            "law_id": law_id,
            "lsi": lsi,  # 온톨로지에 등록된 LOD LSI
            "mst": mst,
            "fetched_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "total_articles": len(articles),
            "source": "https://www.law.go.kr/DRF/lawService.do",
        },
        "articles": articles,
    }
    out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    size_kb = out_path.stat().st_size // 1024
    print(f"  [ok] {len(articles)}개 조문 → {out_path.name} ({size_kb} KB, {time.time()-t0:.1f}s)")
    return {"law_id": law_id, "ok": True, "articles": len(articles), "size_kb": size_kb}


def main():
    t_start = time.time()
    targets = SPECIAL_LAWS
    if TARGET_LAW_ID:
        targets = [e for e in SPECIAL_LAWS if e["law_id"] == TARGET_LAW_ID]
        if not targets:
            print(f"[error] unknown law_id: {TARGET_LAW_ID}", file=sys.stderr)
            sys.exit(2)

    print(f"===== 특별법 인제스트 시드 수집 시작: {len(targets)}개 =====")
    results = []
    for i, entry in enumerate(targets, 1):
        print(f"[{i}/{len(targets)}] {entry['law_name']}")
        results.append(process_law(entry))
        # rate limit 회피: 법령 간 1초 대기
        if i < len(targets):
            time.sleep(1.0)

    # 요약
    ok = [r for r in results if r.get("ok")]
    failed = [r for r in results if not r.get("ok")]
    total_articles = sum(r.get("articles", 0) for r in ok)
    print()
    print(f"===== 완료: {len(ok)}/{len(targets)} 성공, 총 조문 {total_articles}개, {time.time()-t_start:.1f}s =====")
    if failed:
        print("실패 목록:")
        for r in failed:
            print(f"  - {r['law_id']}: {r.get('error')}")
        sys.exit(1)


if __name__ == "__main__":
    main()
