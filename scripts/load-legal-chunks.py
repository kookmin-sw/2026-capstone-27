#!/usr/bin/env python3
"""
civil_law_ontology.rdf → MongoDB legal_chunks 컬렉션 메타데이터 적재.

RDF에서 법률 메타데이터(법령ID, 법령명, 조문번호, 시행일 등)를 추출하여
legal_chunks 컬렉션에 삽입. 조문 본문(content)은 미포함 — 별도 OpenAPI 스크립트로 채움.

사용법:
    python load-legal-chunks.py --rdf-path /path/to/civil_law_ontology.rdf
    python load-legal-chunks.py --mongodb-uri mongodb://localhost:27017/shield --rdf-path ./civil_law_ontology.rdf

요구사항:
    pip install pymongo
"""

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from pymongo import MongoClient, ASCENDING, TEXT

# RDF 네임스페이스
NS = {
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "j.0": "http://lod.law.go.kr/Class/",
    "j.1": "http://lod.law.go.kr/property/",
}

LOD_BASE = "http://lod.law.go.kr/resource/"
ARTICLE_PATTERN = re.compile(r"articleType_(LSI\d+)_(\d+)_(\d+)")
LEGISLATION_TERM_PATTERN = re.compile(r"legislationTermType_(\d+)")


def parse_rdf(rdf_path: str) -> list[dict]:
    """RDF 파일에서 법률 및 조문 메타데이터 추출."""
    tree = ET.parse(rdf_path)
    root = tree.getroot()

    laws = {}  # law_id -> law metadata
    chunks = []

    # KoreanLegislationNorms 요소 파싱
    for elem in root.findall("j.0:KoreanLegislationNorms", NS):
        about = elem.get(f"{{{NS['rdf']}}}about", "")
        law_id = about.replace(LOD_BASE, "") if about.startswith(LOD_BASE) else None
        if not law_id:
            continue

        law_name_elem = elem.find("j.1:lawName", NS)
        law_name = law_name_elem.text if law_name_elem is not None else ""

        label_elem = elem.find("rdfs:label", NS)
        label = label_elem.text if label_elem is not None else law_name

        enforce_date_elem = elem.find("j.1:enforceDate", NS)
        enforce_date = (
            enforce_date_elem.text if enforce_date_elem is not None else None
        )

        law_code_elem = elem.find("j.1:lawCode", NS)
        law_code = law_code_elem.text if law_code_elem is not None else None

        # LOD URI
        lod_uri = about

        # 조문 목록 추출
        articles = []
        for art_elem in elem.findall("j.1:hasArticleCategory", NS):
            art_resource = art_elem.get(f"{{{NS['rdf']}}}resource", "")
            match = ARTICLE_PATTERN.search(art_resource)
            if match:
                art_law_id = match.group(1)
                art_no = match.group(2).lstrip("0") or "0"
                art_sub = match.group(3)
                articles.append(
                    {
                        "law_id": art_law_id,
                        "article_no": f"제{art_no}조"
                        + (f"의{art_sub}" if art_sub != "00" else ""),
                        "article_resource": art_resource,
                    }
                )

        # 법령 용어 추출
        legislation_terms = []
        for term_elem in elem.findall("j.1:hasLegislationTermCategory", NS):
            term_resource = term_elem.get(f"{{{NS['rdf']}}}resource", "")
            term_match = LEGISLATION_TERM_PATTERN.search(term_resource)
            if term_match:
                legislation_terms.append(term_resource.replace(LOD_BASE, ""))

        # 법률 정보 저장
        laws[law_id] = {
            "law_id": law_id,
            "law_name": law_name or label,
            "effective_date": enforce_date,
            "lod_uri": lod_uri,
            "law_code": law_code,
            "legislation_terms": legislation_terms,
        }

        # 각 조문을 chunk로 변환
        for art in articles:
            chunk_id = f"{law_id}_{art['article_resource'].split('_')[-2]}_{art['article_resource'].split('_')[-1]}"
            chunks.append(
                {
                    "_id": chunk_id,
                    "law_id": law_id,
                    "law_name": law_name or label,
                    "article_no": art["article_no"],
                    "article_title": "",  # RDF에 조문 제목 미포함 — 추후 OpenAPI로 채움
                    "content": "",  # 조문 본문 미포함 — 추후 OpenAPI로 채움
                    "effective_date": enforce_date,
                    "source_url": f"https://www.law.go.kr/법령/{law_name or label}",
                    "abolition_date": None,
                    "category_ids": [],
                    "embedding": None,
                    "lod_uri": lod_uri,
                    "legislation_terms": legislation_terms[:10],  # 상위 10개만
                }
            )

    return chunks


def ensure_text_index(collection):
    """content + article_title 텍스트 인덱스 생성 (없으면)."""
    existing_indexes = collection.index_information()
    has_text_index = any(
        any(field_type == "text" for _, field_type in idx.get("key", []))
        for idx in existing_indexes.values()
    )

    if not has_text_index:
        print("텍스트 인덱스 생성 중: (content, article_title)...")
        collection.create_index(
            [("content", TEXT), ("article_title", TEXT)],
            default_language="none",
            name="text_content_article_title",
        )
        print("텍스트 인덱스 생성 완료")
    else:
        print("텍스트 인덱스 이미 존재")

    # law_id 인덱스
    if "idx_law_id" not in existing_indexes:
        print("law_id 인덱스 생성 중...")
        collection.create_index([("law_id", ASCENDING)], name="idx_law_id")
        print("law_id 인덱스 생성 완료")


def main():
    parser = argparse.ArgumentParser(
        description="civil_law_ontology.rdf → MongoDB legal_chunks 메타데이터 적재"
    )
    parser.add_argument(
        "--mongodb-uri",
        default="mongodb://localhost:27017/shield",
        help="MongoDB 연결 URI (default: mongodb://localhost:27017/shield)",
    )
    parser.add_argument(
        "--rdf-path",
        required=True,
        help="civil_law_ontology.rdf 파일 경로",
    )
    args = parser.parse_args()

    rdf_path = Path(args.rdf_path)
    if not rdf_path.exists():
        print(f"ERROR: RDF 파일을 찾을 수 없습니다: {rdf_path}", file=sys.stderr)
        sys.exit(1)

    print(f"RDF 파싱 중: {rdf_path}")
    chunks = parse_rdf(str(rdf_path))
    print(f"파싱 완료: {len(chunks)}개 법률 조문 청크")

    if not chunks:
        print("WARNING: 파싱된 청크가 없습니다.", file=sys.stderr)
        sys.exit(1)

    # MongoDB 연결
    client = MongoClient(args.mongodb_uri)
    db_name = args.mongodb_uri.rsplit("/", 1)[-1].split("?")[0]
    db = client[db_name]
    collection = db["legal_chunks"]

    # 텍스트 인덱스 확인/생성
    ensure_text_index(collection)

    # 벌크 upsert
    from pymongo import ReplaceOne

    operations = [
        ReplaceOne({"_id": chunk["_id"]}, chunk, upsert=True) for chunk in chunks
    ]

    batch_size = 500
    total_upserted = 0
    for i in range(0, len(operations), batch_size):
        batch = operations[i : i + batch_size]
        result = collection.bulk_write(batch)
        total_upserted += result.upserted_count + result.modified_count
        print(f"  배치 {i // batch_size + 1}: {len(batch)}건 처리")

    # 법률 수 확인
    law_count = len(collection.distinct("law_id"))
    print(f"\n완료: {total_upserted}건 upsert, {law_count}개 법률")
    client.close()


if __name__ == "__main__":
    main()
