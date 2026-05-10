#!/usr/bin/env python3
"""
슬림 온톨로지 JSON 생성 스크립트 (R-11).
원본 커스텀 JSON(180노드) → 슬림 버전(id+name+aliases, minified).
- slug 제거
- 빈 children 제거
- aliases(a) 최대 4개 제한
- 키 축약: children→c, aliases→a
"""
import json
import sys
import os


def to_slim(node):
    """노드를 슬림 형식으로 변환."""
    slim = {"id": node["id"], "name": node["name"]}

    # aliases (a) — 원본에 있으면 포함, 최대 4개
    aliases = node.get("aliases", [])
    if aliases:
        slim["a"] = aliases[:4]

    # children (c) — 재귀 변환
    children = node.get("children", [])
    if children:
        slim["c"] = [to_slim(child) for child in children]

    return slim


def count_nodes(node):
    count = 1
    for child in node.get("c", node.get("children", [])):
        count += count_nodes(child)
    return count


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)

    input_path = os.path.join(project_root, "original-ontology.json")
    output_path = os.path.join(
        project_root, "src", "main", "resources", "ontology", "legal-ontology-slim.json"
    )

    if len(sys.argv) > 1:
        input_path = sys.argv[1]
    if len(sys.argv) > 2:
        output_path = sys.argv[2]

    with open(input_path, "r", encoding="utf-8") as f:
        original = json.load(f)

    slim = to_slim(original)

    # Minified JSON 출력
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(slim, f, ensure_ascii=False, separators=(",", ":"))

    total = count_nodes(slim)
    file_size = os.path.getsize(output_path)
    print(f"✓ 슬림 온톨로지 생성 완료")
    print(f"  노드: {total}")
    print(f"  파일: {output_path}")
    print(f"  크기: {file_size:,} bytes ({file_size/1024:.1f} KB)")


if __name__ == "__main__":
    main()
