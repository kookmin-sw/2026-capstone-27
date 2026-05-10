#!/usr/bin/env python3
"""
국가법령정보 판례 OpenAPI에서 민법·특별법 관련 판례 본문을 수집해 시드 JSON으로 저장.

출력: src/main/resources/seed/cases/<slug>.json
  - legal_cases 테이블 스키마와 1:1 대응 (seed/cases/_SCHEMA.md 참조)
  - 판례 1건 = 파일 1개
  - <br/> 태그 제거 + 공백 정규화
  - 참조조문을 ["민법 제312조의2", ...] 배열로 파싱
  - (case_no, court, decision_date) 자연키 중복 스킵

전략:
  1. 법령별 키워드 세트로 lawSearch.do?target=prec 검색 → 상위 N건 판례일련번호 수집
  2. 수집된 판례일련번호마다 lawService.do?target=prec&ID=... 로 본문 취득
  3. 본문을 정규화하여 시드 JSON 저장

재실행 멱등. 기존 파일이 있으면 덮어쓰기 (본문 최신화 가능).

실행:
    LAW_OC=<email_id> python3 scripts/fetch_cases.py
    LAW_OC=<email_id> python3 scripts/fetch_cases.py --limit 50       # 테스트용 축소
    LAW_OC=<email_id> python3 scripts/fetch_cases.py --supreme-only   # 대법원만
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import urlencode

OC = os.environ.get("LAW_OC")
if not OC:
    print("LAW_OC 환경변수 필요", file=sys.stderr)
    sys.exit(2)

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "src/main/resources/seed/cases"
OUT_DIR.mkdir(parents=True, exist_ok=True)

# ------------------------------------------------------------------
# 검색 키워드 세트 — 각 항목은 (keyword, topic_label, category_hints)
# keyword: lawSearch에 그대로 전달되는 검색어
# topic_label: 수집 로그용
# category_hints: 시드 JSON의 category_ids에 붙일 온톨로지 group 토큰
# ------------------------------------------------------------------
SEARCH_QUERIES: list[dict] = [
    # --- 주거/임대차 영역 ---
    {"q": "주택임대차보호법", "topic": "주택임대차", "cats": ["group:leasing", "group:jeonse"]},
    {"q": "상가건물 임대차보호법", "topic": "상가임대차", "cats": ["group:leasing"]},
    {"q": "임차권등기명령", "topic": "임차권등기", "cats": ["group:leasing", "group:jeonse"]},
    {"q": "전세사기", "topic": "전세사기", "cats": ["group:jeonse", "group:leasing"]},
    {"q": "대항력 임차인", "topic": "대항력", "cats": ["group:leasing", "group:jeonse"]},
    {"q": "보증금 반환", "topic": "보증금반환", "cats": ["group:leasing", "group:jeonse"]},

    # --- 부동산 소유/담보 ---
    {"q": "소유권이전등기", "topic": "소유권이전", "cats": ["group:ownership"]},
    {"q": "취득시효", "topic": "취득시효", "cats": ["group:ownership"]},
    {"q": "근저당권", "topic": "근저당", "cats": ["group:mortgage"]},
    {"q": "부동산 실권리자명의", "topic": "명의신탁", "cats": ["group:ownership"]},

    # --- 계약 일반 / 채권 ---
    {"q": "하자담보책임", "topic": "하자담보", "cats": ["group:contract_warranty"]},
    {"q": "계약 해제 원상회복", "topic": "계약해제", "cats": ["group:contract_termination"]},
    {"q": "동시이행", "topic": "동시이행", "cats": ["group:contract_performance"]},
    {"q": "손해배상 예정", "topic": "손해배상예정", "cats": ["group:damages"]},
    {"q": "채무불이행 손해배상", "topic": "채무불이행", "cats": ["group:damages"]},
    {"q": "상계", "topic": "상계", "cats": ["group:set_off"]},
    {"q": "소멸시효", "topic": "소멸시효", "cats": ["group:prescription"]},
    {"q": "소멸시효 중단", "topic": "시효중단", "cats": ["group:prescription"]},

    # --- 보증 ---
    {"q": "보증인 보호", "topic": "보증인보호", "cats": ["group:suretyship"]},
    {"q": "수탁보증인 구상권", "topic": "보증구상", "cats": ["group:suretyship"]},

    # --- 불법행위 ---
    {"q": "위자료 정신적 고통", "topic": "위자료", "cats": ["group:tort"]},
    {"q": "자동차손해배상", "topic": "자동차사고", "cats": ["group:tort"]},
    {"q": "불법행위 손해배상", "topic": "불법행위", "cats": ["group:tort"]},

    # --- 친족/상속 ---
    {"q": "재판상 이혼", "topic": "재판이혼", "cats": ["group:divorce"]},
    {"q": "재산분할", "topic": "재산분할", "cats": ["group:divorce"]},
    {"q": "친권 양육권", "topic": "친권", "cats": ["group:parental_rights"]},
    {"q": "양육비", "topic": "양육비", "cats": ["group:parental_rights"]},
    {"q": "상속 한정승인", "topic": "한정승인", "cats": ["group:inheritance"]},
    {"q": "상속 포기", "topic": "상속포기", "cats": ["group:inheritance"]},
    {"q": "유류분", "topic": "유류분", "cats": ["group:inheritance_reserve"]},
    {"q": "자필증서 유언", "topic": "유언", "cats": ["group:inheritance"]},
    {"q": "상속순위", "topic": "상속순위", "cats": ["group:inheritance"]},

    # --- 회생/파산/집행 ---
    {"q": "개인회생", "topic": "개인회생", "cats": ["group:rehabilitation"]},
    {"q": "파산선고", "topic": "파산", "cats": ["group:bankruptcy"]},
    {"q": "강제집행", "topic": "강제집행", "cats": ["group:civil_exec"]},

    # --- 개인정보 ---
    {"q": "개인정보 보호법", "topic": "개인정보", "cats": ["group:privacy"]},

    # --- 보강 키워드 (추가 판례 확보용) ---
    {"q": "임대차 갱신", "topic": "임대차갱신", "cats": ["group:leasing"]},
    {"q": "확정일자", "topic": "확정일자", "cats": ["group:leasing", "group:jeonse"]},
    {"q": "우선변제권", "topic": "우선변제", "cats": ["group:leasing", "group:jeonse"]},
    {"q": "저당권", "topic": "저당권", "cats": ["group:mortgage"]},
    {"q": "가등기", "topic": "가등기", "cats": ["group:ownership"]},
    {"q": "동시이행 항변", "topic": "동시이행항변", "cats": ["group:contract_performance"]},
    {"q": "해제조건", "topic": "해제조건", "cats": ["group:contract_performance"]},
    {"q": "이행불능", "topic": "이행불능", "cats": ["group:contract_performance"]},
    {"q": "공동불법행위", "topic": "공동불법행위", "cats": ["group:tort"]},
    {"q": "상속회복청구", "topic": "상속회복", "cats": ["group:inheritance"]},
    {"q": "유책배우자", "topic": "유책배우자", "cats": ["group:divorce"]},
    {"q": "양자 파양", "topic": "파양", "cats": ["group:parental_rights"]},
    {"q": "채권자 취소권", "topic": "채권자취소", "cats": ["group:contract_termination"]},
    {"q": "부당이득", "topic": "부당이득", "cats": ["group:damages"]},
    {"q": "조정조서", "topic": "조정", "cats": ["group:civil_exec"]},
    {"q": "무효 설정등기", "topic": "무효등기", "cats": ["group:ownership", "group:mortgage"]},
]

PER_QUERY_LIMIT = 30  # 쿼리당 상위 몇 건 수집할지


_COOKIE_JAR = str((ROOT / ".cache" / "law.go.kr.cookies").resolve())
os.makedirs(os.path.dirname(_COOKIE_JAR), exist_ok=True)


def _curl_xml(url: str) -> str:
    """GET 후 원문 XML 문자열 반환. 4회 재시도 + 세션 쿠키 공유.

    law.go.kr은 연속 요청 시 간헐적으로 TLS 핸드셰이크를 끊는 경우가 있어
    --cookie-jar / --cookie로 JSESSIONID를 공유하고 재시도 간격을 늘린다."""
    last_err = None
    for attempt in range(4):
        proc = subprocess.run(
            ["curl", "-sS", "--max-time", "60",
             "--cookie-jar", _COOKIE_JAR, "--cookie", _COOKIE_JAR,
             "-H", "User-Agent: Mozilla/5.0 SHIELD-ingest/1.0",
             url],
            capture_output=True, text=True,
        )
        if proc.returncode == 0 and proc.stdout.strip().startswith("<?xml"):
            # "일치하는 판례가 없습니다" 같은 빈 응답도 XML로 내려옵으나
            # <Law>...</Law> 태그로 감싸져 나오므로 여기서도 그대로 반환하고
            # 호출측에서 유효성을 판단한다 (PrecService 루트 여부).
            return proc.stdout
        last_err = f"rc={proc.returncode} head={proc.stdout[:120]}"
        print(f"  [warn] attempt {attempt+1} failed: {last_err}", file=sys.stderr)
        time.sleep(2 + attempt * 2)  # 2s, 4s, 6s, 8s
    raise RuntimeError(f"law.go.kr 요청 실패: {last_err}")


def _search_prec(query: str, page: int = 1, display: int = 20) -> list[dict]:
    """판례 목록 검색. 반환: [{precSeq, caseName, caseNo, date, court, caseType}]"""
    params = urlencode({
        "OC": OC,
        "target": "prec",
        "type": "XML",
        "query": query,
        "display": display,
        "page": page,
    })
    url = f"https://www.law.go.kr/DRF/lawSearch.do?{params}"
    xml = _curl_xml(url)
    root = ET.fromstring(xml)
    out = []
    for prec in root.findall("prec"):
        item = {
            "prec_seq": (prec.findtext("판례일련번호") or "").strip(),
            "case_name": (prec.findtext("사건명") or "").strip(),
            "case_no": (prec.findtext("사건번호") or "").strip(),
            "decision_date": (prec.findtext("선고일자") or "").strip(),
            "court": (prec.findtext("법원명") or "").strip(),
            "case_type": (prec.findtext("사건종류명") or "").strip(),
            "judgment_type": (prec.findtext("판결유형") or "").strip(),
            "source_name": (prec.findtext("데이터출처명") or "").strip(),
        }
        if item["prec_seq"] and item["case_no"]:
            out.append(item)
    return out


def _fetch_prec_detail(prec_seq: str) -> dict | None:
    """판례 본문 XML 1건을 파싱. 는 경우 None.

    법제처 API는 대법원 판례만 본문 전문을 제공하고, 국세법령정보시스템 원첵 등
    하급심 판례는 목록만 있고 상세 호출 시 <Law>일치하는 판례가 없습니다</Law>로
    응답한다. 이 경우 None 반환."""
    params = urlencode({
        "OC": OC,
        "target": "prec",
        "type": "XML",
        "ID": prec_seq,
    })
    url = f"https://www.law.go.kr/DRF/lawService.do?{params}"
    xml = _curl_xml(url)
    root = ET.fromstring(xml)
    if root.tag != "PrecService":
        return None
    return {
        "prec_seq": (root.findtext("판례정보일련번호") or "").strip(),
        "case_name": (root.findtext("사건명") or "").strip(),
        "case_no": (root.findtext("사건번호") or "").strip(),
        "decision_date": (root.findtext("선고일자") or "").strip(),
        "court": (root.findtext("법원명") or "").strip(),
        "case_type": (root.findtext("사건종류명") or "").strip(),
        "judgment_type": (root.findtext("판결유형") or "").strip(),
        "disposition": (root.findtext("선고") or "").strip(),
        "headnote": (root.findtext("판시사항") or "").strip(),
        "holding": (root.findtext("판결요지") or "").strip(),
        "references": (root.findtext("참조조문") or "").strip(),
        "cited_cases_raw": (root.findtext("참조판례") or "").strip(),
        "full_text": (root.findtext("판례내용") or "").strip(),
    }


def _clean_text(s: str) -> str:
    """<br/> → 줄바꿈, 중복 공백 정리."""
    if not s:
        return ""
    s = re.sub(r"<br\s*/?>", "\n", s, flags=re.I)
    s = re.sub(r"&nbsp;", " ", s)
    s = re.sub(r"[ \t]+", " ", s)
    s = re.sub(r"\n\s*\n\s*\n+", "\n\n", s)
    return s.strip()


def _parse_cited_articles(references_raw: str) -> list[str]:
    """참조조문 원문 → [\"민법 제312조의2\", ...] 배열.

    원문 예시: "주택임대차보호법 제3조의3 제3항, 제8항, 민사소송법 제110조, 민법 제492조 제1항"
    """
    text = _clean_text(references_raw)
    if not text:
        return []
    # 각 조문 개별 항목으로 쪼개기 — ", " 또는 줄바꿈 단위
    # 단, "제3조의3 제3항, 제8항" 같은 항 나열은 같은 조문으로 유지하기 어렵기 때문에
    # 쉼표 단위 분할 후 각 토큰을 조문으로 본다. 이후 필요 시 정규화.
    items = re.split(r"[,\n]+", text)
    out = []
    current_law: str | None = None
    current_article: str | None = None  # 가장 최근의 "제N조" 본체 (항 분리를 위함)
    for raw in items:
        tok = raw.strip()
        if not tok:
            continue
        # "법/령/규칙" 등으로 끝나는 법령명 포함 여부
        m = re.match(r"^(.+?(?:법|령|규칙|특별법|시행령|시행규칙|헌법))\s+(제.+)$", tok)
        if m:
            current_law = m.group(1)
            art = m.group(2).strip()
            # 조 단위 추출: "제477조", "제312조의2"
            jo_match = re.match(r"^(제\d+조(?:의\d+)?)", art)
            current_article = jo_match.group(1) if jo_match else None
            out.append(f"{current_law} {art}")
        elif current_law and tok.startswith("제") and re.match(r"^제\d+조", tok):
            # 같은 법령의 다른 조 (ex: "제478조")
            art = tok
            jo_match = re.match(r"^(제\d+조(?:의\d+)?)", art)
            current_article = jo_match.group(1) if jo_match else None
            out.append(f"{current_law} {art}")
        elif current_law and current_article and tok.startswith("제") and re.match(r"^제\d+항", tok):
            # 앞 조의 다른 항 (ex: "제2항") — 조 재사용
            out.append(f"{current_law} {current_article} {tok}")
        elif current_law and tok.startswith("제"):
            # 기타 (예: "제1호") — 조가 있으면 조와 결합, 없으면 법령만
            if current_article:
                out.append(f"{current_law} {current_article} {tok}")
            else:
                out.append(f"{current_law} {tok}")
        elif tok.startswith("제"):
            # 법령명 없는 조문 (드묾). 스킵.
            continue
    # 중복 제거, 순서 유지
    seen = set()
    uniq = []
    for x in out:
        if x not in seen:
            seen.add(x)
            uniq.append(x)
    return uniq


def _parse_cited_cases(raw: str) -> list[str]:
    """참조판례 원문 → 사건번호 배열. 예: '대법원 2002. 11. 8. 선고 2002다38361 판결'"""
    text = _clean_text(raw)
    if not text:
        return []
    # '2002다38361', '2020나12345' 같은 패턴 추출
    return re.findall(r"\d{4}[가-힣]+\d+", text)


def _normalize_date(d: str) -> str:
    """'20250424' → '2025-04-24'. 이미 ISO면 그대로."""
    if re.match(r"^\d{8}$", d):
        return f"{d[:4]}-{d[4:6]}-{d[6:8]}"
    if re.match(r"^\d{4}[.\-]\d{2}[.\-]\d{2}", d):
        return d[:4] + "-" + d[5:7] + "-" + d[8:10]
    return d


def _slugify(case_no: str, court: str, date_iso: str) -> str:
    """파일명 안전. 한글은 로마자/코드 대체 대신 그대로 두되 특수문자만 제거."""
    safe_case_no = re.sub(r"[^\w가-힣]", "_", case_no)
    safe_court = re.sub(r"[^\w가-힣]", "", court)
    return f"{date_iso}_{safe_court}_{safe_case_no}"


def _to_seed_record(detail: dict, hints: list[str]) -> dict:
    date_iso = _normalize_date(detail["decision_date"])
    case_no = detail["case_no"]
    # 법원명이 비어있는 하급심도 있음 — 데이터 출처로 보완 시도
    court = detail["court"] or "하급심"
    case_name = _clean_text(detail["case_name"])
    headnote = _clean_text(detail["headnote"])
    holding = _clean_text(detail["holding"])
    full_text = _clean_text(detail["full_text"])
    # 판결이유 섹션 발췌 — full_text에서 "【이유】" 이후 일부
    reasoning = ""
    m = re.search(r"【\s*이\s*유\s*】([\s\S]+)$", full_text)
    if m:
        reasoning = _clean_text(m.group(1))[:4000]  # 길이 제한
    cited_articles = _parse_cited_articles(detail["references"])
    cited_cases = _parse_cited_cases(detail["cited_cases_raw"])

    return {
        "meta": {
            "source": "law.go.kr",
            "source_id": detail["prec_seq"],
            "source_url": f"https://www.law.go.kr/DRF/lawService.do?OC={OC}&target=prec&ID={detail['prec_seq']}&type=HTML",
            "fetched_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        },
        "case": {
            "case_no": case_no,
            "court": court,
            "case_name": case_name or None,
            "decision_date": date_iso,
            "case_type": detail["case_type"] or "민사",
            "judgment_type": detail["judgment_type"] or None,
            "disposition": None,  # 원문 내 "주문" 섹션은 full_text 안에 있어 별도 파싱 생략
            "headnote": headnote or None,
            "holding": holding or None,
            "reasoning": reasoning or None,
            "full_text": full_text or None,
            "cited_articles": cited_articles,
            "cited_cases": cited_cases,
            "category_ids": sorted(set(hints)),
        },
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=None, help="전체 수집 상한 (테스트용)")
    ap.add_argument("--per-query", type=int, default=PER_QUERY_LIMIT, help="쿼리당 상위 N건")
    ap.add_argument("--supreme-only", action="store_true", help="대법원 판결만 유지")
    ap.add_argument("--min-holding-len", type=int, default=30, help="판결요지 최소 길이")
    args = ap.parse_args()

    collected: dict[str, dict] = {}   # prec_seq → detail
    hints_by_seq: dict[str, set[str]] = {}  # prec_seq → category hints

    print(f"[INFO] {len(SEARCH_QUERIES)}개 키워드로 판례 목록 검색 시작", file=sys.stderr)
    for q in SEARCH_QUERIES:
        try:
            hits = _search_prec(q["q"], page=1, display=args.per_query)
        except Exception as e:
            print(f"  [WARN] '{q['q']}' 검색 실패: {e}", file=sys.stderr)
            continue
        print(f"  - [{q['topic']:>10}] '{q['q']}' → {len(hits)}건", file=sys.stderr)
        for h in hits:
            seq = h["prec_seq"]
            if args.supreme_only and h["court"] != "대법원":
                continue
            if seq in collected:
                hints_by_seq[seq].update(q["cats"])
                continue
            collected[seq] = h
            hints_by_seq[seq] = set(q["cats"])
        time.sleep(0.25)

    if args.limit:
        seqs = list(collected.keys())[:args.limit]
        collected = {s: collected[s] for s in seqs}
    print(f"[INFO] 고유 판례 {len(collected)}건 → 본문 수집", file=sys.stderr)

    natural_keys_seen: set[tuple] = set()
    saved = 0
    skipped_short = 0
    skipped_dup = 0
    failed = 0

    no_body = 0
    for i, (seq, hdr) in enumerate(collected.items(), 1):
        try:
            detail = _fetch_prec_detail(seq)
        except Exception as e:
            print(f"  [WARN] {seq} 본문 요청 실패: {e}", file=sys.stderr)
            failed += 1
            continue
        time.sleep(0.4)
        if detail is None:
            no_body += 1
            continue

        # 본문 품질 가드: 판결요지 길이 충분해야 인제스트 대상
        if len(_clean_text(detail["holding"])) < args.min_holding_len:
            skipped_short += 1
            continue

        rec = _to_seed_record(detail, sorted(hints_by_seq[seq]))
        nk = (rec["case"]["case_no"], rec["case"]["court"], rec["case"]["decision_date"])
        if nk in natural_keys_seen:
            skipped_dup += 1
            continue
        natural_keys_seen.add(nk)

        slug = _slugify(rec["case"]["case_no"], rec["case"]["court"], rec["case"]["decision_date"])
        out_path = OUT_DIR / f"{slug}.json"
        with out_path.open("w", encoding="utf-8") as f:
            json.dump(rec, f, ensure_ascii=False, indent=2)
        saved += 1
        if saved % 25 == 0:
            print(f"  [{i}/{len(collected)}] saved={saved} short={skipped_short} dup={skipped_dup} fail={failed}", file=sys.stderr)

    print(f"[DONE] 저장 {saved}건, 짧음 {skipped_short}건, 자연키중복 {skipped_dup}건, 본문없음 {no_body}건, 실패 {failed}건", file=sys.stderr)
    print(f"       출력 디렉터리: {OUT_DIR}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
