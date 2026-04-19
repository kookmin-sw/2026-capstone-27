"""Generate civil-law-category-map.yml from civil-law.json.

민법 seed JSON의 book/chapter/section 구조를 바탕으로 RAG 카테고리 맵을
자동 생성한다. 본 파일은 B-2 인제스트 파이프라인에서 청크 메타데이터로 사용되며,
SHIELD의 임대차/전세 사기 도메인에 필요한 '필수 포함 그룹'도 함께 정의한다.

사용법:
    python scripts/generate_civil_law_category_map.py

출력:
    src/main/resources/seed/civil-law-category-map.yml
"""

from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
SEED_JSON = ROOT / "src/main/resources/seed/civil-law.json"
OUTPUT_YML = ROOT / "src/main/resources/seed/civil-law-category-map.yml"


def load_articles() -> list[dict[str, Any]]:
    with SEED_JSON.open("r", encoding="utf-8") as f:
        data = json.load(f)
    return data["articles"]


def compute_ranges(
    articles: list[dict[str, Any]],
) -> tuple[dict[str, tuple[int, int]], dict[tuple[str, str], tuple[int, int]], dict[tuple[str, str, str], tuple[int, int]]]:
    """book/chapter/section 별 조문 번호 범위를 계산."""
    books: dict[str, list[int]] = defaultdict(list)
    chapters: dict[tuple[str, str], list[int]] = defaultdict(list)
    sections: dict[tuple[str, str, str], list[int]] = defaultdict(list)

    for a in articles:
        no = a.get("article_no_int")
        if no is None:
            continue
        book = a.get("book")
        chapter = a.get("chapter")
        section = a.get("section")
        if book:
            books[book].append(no)
        if book and chapter:
            chapters[(book, chapter)].append(no)
        if book and chapter and section:
            sections[(book, chapter, section)].append(no)

    book_ranges = {b: (min(ns), max(ns)) for b, ns in books.items()}
    chapter_ranges = {k: (min(ns), max(ns)) for k, ns in chapters.items()}
    section_ranges = {k: (min(ns), max(ns)) for k, ns in sections.items()}
    return book_ranges, chapter_ranges, section_ranges


def _esc(s: str) -> str:
    """YAML double-quoted 문자열 escape."""
    return s.replace("\\", "\\\\").replace('"', '\\"')


def emit_yaml(
    book_ranges: dict[str, tuple[int, int]],
    chapter_ranges: dict[tuple[str, str], tuple[int, int]],
    section_ranges: dict[tuple[str, str, str], tuple[int, int]],
) -> str:
    lines: list[str] = []
    lines.append("# 민법 카테고리 맵 (자동 생성 — 수동 수정 금지)")
    lines.append("# 생성 스크립트: scripts/generate_civil_law_category_map.py")
    lines.append("# 원본: src/main/resources/seed/civil-law.json")
    lines.append("")
    lines.append("meta:")
    lines.append('  law_id: "law-civil"')
    lines.append('  law_name: "민법"')
    lines.append('  source_law_id: "001706"')
    lines.append('  source_mst: "284415"')
    lines.append("")

    lines.append("# 편(book) 단위 — 최상위 분류")
    lines.append("books:")
    for book, (lo, hi) in sorted(book_ranges.items(), key=lambda x: x[1][0]):
        lines.append(f'  - name: "{_esc(book)}"')
        lines.append(f"    range: [{lo}, {hi}]")
    lines.append("")

    lines.append("# 장(chapter) 단위 — 중간 분류")
    lines.append("chapters:")
    for (book, chapter), (lo, hi) in sorted(chapter_ranges.items(), key=lambda x: x[1][0]):
        lines.append(f'  - book: "{_esc(book)}"')
        lines.append(f'    name: "{_esc(chapter)}"')
        lines.append(f"    range: [{lo}, {hi}]")
    lines.append("")

    lines.append("# 절(section) 단위 — 세부 분류 (계약/채권 총칙 등)")
    lines.append("sections:")
    for (book, chapter, section), (lo, hi) in sorted(section_ranges.items(), key=lambda x: x[1][0]):
        lines.append(f'  - book: "{_esc(book)}"')
        lines.append(f'    chapter: "{_esc(chapter)}"')
        lines.append(f'    name: "{_esc(section)}"')
        lines.append(f"    range: [{lo}, {hi}]")
    lines.append("")

    # --- 필수 포함 그룹 (B-2 설계 메모) ----------------------------------
    # matched_nodes 상위 N에 그룹 소속 조문이 하나라도 있으면,
    # retrieval 결과에 그룹 전체 조문을 OR 결합해서 포함한다.
    # ----------------------------------------------------------------------
    lines.append("# 필수 포함 그룹 — matched_nodes 후보에 하나라도 걸리면 전체 포함")
    lines.append("mandatory_groups:")
    groups = [
        ("leasing", "임대차", 618, 654, "제3편 채권 > 제2장 계약 > 제7절 임대차"),
        ("jeonse", "전세권", 303, 319, "제2편 물권 > 제6장 전세권"),
        ("guaranty_debtors", "보증 · 다수당사자 채권", 408, 448, "제3편 채권 > 제1장 총칙 > 제3절 수인의 채권자 및 채무자"),
        ("claim_effect", "채권의 효력", 387, 407, "제3편 채권 > 제1장 총칙 > 제2절 채권의 효력"),
        ("mortgage", "저당권", 356, 372, "제2편 물권 > 제9장 저당권"),
        ("pledge", "질권", 329, 355, "제2편 물권 > 제8장 질권"),
        ("ownership", "소유권", 211, 278, "제2편 물권 > 제3장 소유권"),
    ]
    for tag, label, lo, hi, origin in groups:
        lines.append(f'  - tag: "{tag}"')
        lines.append(f'    label: "{_esc(label)}"')
        lines.append(f"    range: [{lo}, {hi}]")
        lines.append(f'    origin: "{_esc(origin)}"')
    lines.append("")

    return "\n".join(lines)


def main() -> None:
    articles = load_articles()
    book_ranges, chapter_ranges, section_ranges = compute_ranges(articles)
    yaml_text = emit_yaml(book_ranges, chapter_ranges, section_ranges)
    OUTPUT_YML.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_YML.write_text(yaml_text, encoding="utf-8")
    print(f"생성 완료: {OUTPUT_YML}")
    print(f"  books:    {len(book_ranges)}")
    print(f"  chapters: {len(chapter_ranges)}")
    print(f"  sections: {len(section_ranges)}")


if __name__ == "__main__":
    main()
