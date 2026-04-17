#!/usr/bin/env python3
"""
category-law-mappings.yml → MongoDB category_law_mappings 컬렉션 마이그레이션.

사용법:
    python migrate-category-law-mappings.py
    python migrate-category-law-mappings.py --mongodb-uri mongodb://localhost:27017/shield
    python migrate-category-law-mappings.py --yaml-path ../src/main/resources/ontology/category-law-mappings.yml

요구사항:
    pip install pymongo pyyaml
"""

import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path

import yaml
from pymongo import MongoClient

# 카테고리 ID 접두사 → 도메인 매핑
DOMAIN_MAP = {
    "law-001": "CIVIL",       # 부동산 거래
    "law-002": "CIVIL",       # 이혼·위자료·재산분할
    "law-003": "CIVIL",       # 상속·유류분·유언
    "law-004": "LABOR",       # 근로계약·해고·임금
    "law-005": "CIVIL",       # 손해배상·불법행위
    "law-006": "CIVIL",       # 채무·보증·개인파산·회생
    "law-007": "CIVIL",       # 임대차보호
    "law-008": "COMMERCIAL",  # 기업·상사거래
}

# 카테고리 ID → 이름 매핑
NAME_MAP = {
    "law-001-01": "부동산 매매",
    "law-001-02": "부동산 임대차",
    "law-001-03": "부동산 담보",
    "law-001-04": "부동산 권리관계",
    "law-002-01": "이혼 절차",
    "law-002-02": "위자료",
    "law-002-03": "재산분할",
    "law-002-04": "자녀 및 양육",
    "law-003-01": "상속 일반",
    "law-003-02": "상속재산 처리",
    "law-003-03": "유언",
    "law-003-04": "유류분",
    "law-004-01": "근로계약",
    "law-004-02": "임금 및 수당",
    "law-004-03": "근로시간 및 휴가",
    "law-004-04": "해고 및 징계",
    "law-004-05": "직장 내 권리보호",
    "law-005-01": "손해배상 일반",
    "law-005-02": "교통사고",
    "law-005-03": "의료사고",
    "law-005-04": "인격권 침해",
    "law-005-05": "특수 불법행위 책임",
    "law-006-01": "금전채권 및 채무",
    "law-006-02": "보증",
    "law-006-03": "민사집행 및 보전처분",
    "law-006-04": "개인파산",
    "law-006-05": "개인회생",
    "law-007-01": "주택임대차보호",
    "law-007-02": "상가건물임대차보호",
    "law-007-03": "임차인 보호 절차",
    "law-008-01": "상사계약 일반",
    "law-008-02": "납품·공급·도급",
    "law-008-03": "유통·가맹·대리점",
    "law-008-04": "지분 및 주주 분쟁",
    "law-008-05": "임원 책임 및 영업보호",
}


def resolve_domain(category_id: str) -> str:
    """카테고리 ID의 접두사로 도메인 결정."""
    prefix = "-".join(category_id.split("-")[:2])
    return DOMAIN_MAP.get(prefix, "UNKNOWN")


def filter_external(law_refs: list[dict]) -> list[str]:
    """EXTERNAL law_id를 제외하고 law_id 목록 반환."""
    if not law_refs:
        return []
    return [ref["law_id"] for ref in law_refs if ref.get("law_id") != "EXTERNAL"]


def main():
    parser = argparse.ArgumentParser(
        description="category-law-mappings.yml → MongoDB 마이그레이션"
    )
    parser.add_argument(
        "--mongodb-uri",
        default="mongodb://localhost:27017/shield",
        help="MongoDB 연결 URI (default: mongodb://localhost:27017/shield)",
    )
    parser.add_argument(
        "--yaml-path",
        default=None,
        help="category-law-mappings.yml 경로 (기본: 스크립트 상대경로에서 탐색)",
    )
    args = parser.parse_args()

    # YAML 파일 경로 결정
    if args.yaml_path:
        yaml_path = Path(args.yaml_path)
    else:
        script_dir = Path(__file__).resolve().parent
        yaml_path = (
            script_dir.parent
            / "src"
            / "main"
            / "resources"
            / "ontology"
            / "category-law-mappings.yml"
        )

    if not yaml_path.exists():
        print(f"ERROR: YAML 파일을 찾을 수 없습니다: {yaml_path}", file=sys.stderr)
        sys.exit(1)

    # YAML 파싱
    with open(yaml_path, encoding="utf-8") as f:
        data = yaml.safe_load(f)

    mappings = data.get("mappings", {})
    if not mappings:
        print("WARNING: mappings 키가 비어있습니다.", file=sys.stderr)
        sys.exit(1)

    print(f"YAML 로드 완료: {len(mappings)}개 카테고리")

    # MongoDB 연결
    client = MongoClient(args.mongodb_uri)
    db_name = args.mongodb_uri.rsplit("/", 1)[-1].split("?")[0]
    db = client[db_name]
    collection = db["category_law_mappings"]

    now = datetime.now(timezone.utc)
    upserted = 0
    skipped = 0

    for category_id, value in mappings.items():
        primary_ids = filter_external(value.get("primary", []))
        secondary_ids = filter_external(value.get("secondary", []))

        # primary_law_ids가 모두 EXTERNAL이면 스킵
        if not primary_ids and not secondary_ids:
            print(f"  SKIP {category_id} (모든 law_id가 EXTERNAL)")
            skipped += 1
            continue

        doc = {
            "_id": category_id,
            "name": NAME_MAP.get(category_id, category_id),
            "domain": resolve_domain(category_id),
            "primary_law_ids": primary_ids,
            "secondary_law_ids": secondary_ids,
            "updated_at": now,
        }

        collection.replace_one({"_id": category_id}, doc, upsert=True)
        upserted += 1

    print(f"\n완료: {upserted}개 upsert, {skipped}개 스킵 (EXTERNAL only)")
    client.close()


if __name__ == "__main__":
    main()
