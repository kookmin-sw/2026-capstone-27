#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
민법 관련 법률의 상세 RDF/XML 트리플을 법제처 LOD에서 다운로드하고,
결합된 RDF/XML 파일과 OWL 온톨로지 파일을 생성.
"""

import json
import sys
import time
import pathlib
import re
import requests

BASE_URL = "https://lod.law.go.kr/DRF/lod/sparql.do"
OUTPUT_DIR = pathlib.Path("lod_law_downloads")
RDF_DIR = OUTPUT_DIR / "rdf_xml"
CIVIL_JSON = OUTPUT_DIR / "civil_law_legislation.json"
COMBINED_RDF = OUTPUT_DIR / "civil_law_ontology.rdf"
OWL_FILE = OUTPUT_DIR / "civil_law_ontology.owl"

VALUES_BATCH_SIZE = 20  # URIs per CONSTRUCT query
SLEEP_SEC = 1.0
TIMEOUT = 120

CONSTRUCT_TEMPLATE = """PREFIX ldc: <http://lod.law.go.kr/Class/>
PREFIX ldp: <http://lod.law.go.kr/property/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>

CONSTRUCT {{
  ?s ?p ?o .
}} WHERE {{
  VALUES ?s {{ {values} }}
  ?s ?p ?o .
}}"""


def fetch_construct(session: requests.Session, uris: list[str]) -> str:
    values = " ".join(f"<{uri}>" for uri in uris)
    query = CONSTRUCT_TEMPLATE.format(values=values)
    resp = session.post(
        BASE_URL,
        data={"query": query, "time_out": "60"},
        headers={"User-Agent": "Mozilla/5.0 (compatible; ClaudeCode-LegalOntologyDownloader/1.0)"},
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    return resp.text


def extract_rdf_body(rdf_xml: str) -> str:
    """rdf:RDF 태그 내부의 리소스 요소들만 추출."""
    # Remove XML declaration
    body = re.sub(r"<\?xml[^?]*\?>", "", rdf_xml).strip()
    # Remove opening rdf:RDF tag
    body = re.sub(r"<rdf:RDF[^>]*>", "", body, count=1).strip()
    # Remove closing rdf:RDF tag
    body = re.sub(r"</rdf:RDF>\s*$", "", body).strip()
    return body


def collect_namespaces(rdf_xml: str) -> dict[str, str]:
    """RDF/XML에서 네임스페이스 선언을 수집."""
    ns = {}
    for match in re.finditer(r'xmlns:([^=]+)="([^"]+)"', rdf_xml):
        prefix, uri = match.group(1), match.group(2)
        ns[prefix] = uri
    return ns


def build_combined_rdf(bodies: list[str], all_ns: dict[str, str]) -> str:
    """수집된 RDF 바디를 하나의 RDF/XML로 결합."""
    ns_decl = "\n".join(f'    xmlns:{p}="{u}"' for p, u in sorted(all_ns.items()))
    combined_body = "\n\n".join(bodies)

    return f"""<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
{ns_decl}>

{combined_body}

</rdf:RDF>
"""


def build_owl(bodies: list[str], all_ns: dict[str, str]) -> str:
    """OWL 온톨로지 파일 생성 (클래스/프로퍼티 정의 + 인스턴스)."""
    all_ns["owl"] = "http://www.w3.org/2002/07/owl#"
    all_ns["dc"] = "http://purl.org/dc/elements/1.1/"
    all_ns["xsd"] = "http://www.w3.org/2001/XMLSchema#"

    ns_decl = "\n".join(f'    xmlns:{p}="{u}"' for p, u in sorted(all_ns.items()))
    combined_body = "\n\n".join(bodies)

    return f"""<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
{ns_decl}
    xml:base="http://lod.law.go.kr/ontology/civil-law">

  <!-- Ontology Header -->
  <owl:Ontology rdf:about="http://lod.law.go.kr/ontology/civil-law">
    <dc:title xml:lang="ko">민법 관련 법률 온톨로지</dc:title>
    <dc:description xml:lang="ko">법제처 LOD에서 수집한 민법 관련 법률 온톨로지. 부동산, 임대차, 상속, 채권, 손해배상 등 민사법 영역 법률 포함.</dc:description>
    <dc:source>https://lod.law.go.kr</dc:source>
    <dc:date>2026-04-17</dc:date>
  </owl:Ontology>

  <!-- Class Definitions -->
  <owl:Class rdf:about="http://lod.law.go.kr/Class/KoreanLegislation">
    <rdfs:label xml:lang="ko">대한민국 법률</rdfs:label>
    <rdfs:comment xml:lang="ko">법제처 LOD에 등록된 대한민국 법률 리소스</rdfs:comment>
  </owl:Class>

  <owl:Class rdf:about="http://lod.law.go.kr/Class/KoreanLegislationNorms">
    <rdfs:subClassOf rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
    <rdfs:label xml:lang="ko">대한민국 법률 규범</rdfs:label>
  </owl:Class>

  <!-- Property Definitions -->
  <owl:DatatypeProperty rdf:about="http://lod.law.go.kr/property/lawName">
    <rdfs:label xml:lang="ko">법률명</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
    <rdfs:range rdf:resource="http://www.w3.org/2001/XMLSchema#string"/>
  </owl:DatatypeProperty>

  <owl:DatatypeProperty rdf:about="http://lod.law.go.kr/property/lawCode">
    <rdfs:label xml:lang="ko">법률 코드</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
    <rdfs:range rdf:resource="http://www.w3.org/2001/XMLSchema#integer"/>
  </owl:DatatypeProperty>

  <owl:DatatypeProperty rdf:about="http://lod.law.go.kr/property/announceDate">
    <rdfs:label xml:lang="ko">공포일</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
    <rdfs:range rdf:resource="http://www.w3.org/2001/XMLSchema#dateTime"/>
  </owl:DatatypeProperty>

  <owl:DatatypeProperty rdf:about="http://lod.law.go.kr/property/enforceDate">
    <rdfs:label xml:lang="ko">시행일</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
    <rdfs:range rdf:resource="http://www.w3.org/2001/XMLSchema#dateTime"/>
  </owl:DatatypeProperty>

  <owl:DatatypeProperty rdf:about="http://lod.law.go.kr/property/announceNo">
    <rdfs:label xml:lang="ko">공포 번호</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
    <rdfs:range rdf:resource="http://www.w3.org/2001/XMLSchema#integer"/>
  </owl:DatatypeProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasArticleCategory">
    <rdfs:label xml:lang="ko">조문 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasLegislationTermCategory">
    <rdfs:label xml:lang="ko">법률 용어 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasLegislationKindCategory">
    <rdfs:label xml:lang="ko">법률 종류 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasReformCategory">
    <rdfs:label xml:lang="ko">개정 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasDivisionCategory">
    <rdfs:label xml:lang="ko">소관부처 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasAdditionalRulesCategory">
    <rdfs:label xml:lang="ko">부칙 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <owl:ObjectProperty rdf:about="http://lod.law.go.kr/property/hasKoreanAdminAgencyCategory">
    <rdfs:label xml:lang="ko">행정기관 분류</rdfs:label>
    <rdfs:domain rdf:resource="http://lod.law.go.kr/Class/KoreanLegislation"/>
  </owl:ObjectProperty>

  <!-- ===== Individuals (민법 관련 법률 인스턴스) ===== -->

{combined_body}

</rdf:RDF>
"""


def main():
    if sys.platform == "win32":
        sys.stdout.reconfigure(encoding="utf-8")

    RDF_DIR.mkdir(parents=True, exist_ok=True)

    # Load civil law list
    with CIVIL_JSON.open(encoding="utf-8") as f:
        civil_laws = json.load(f)

    uris = [r["resource_uri"] for r in civil_laws if r["resource_uri"]]
    print(f"[INFO] {len(uris)} civil law resources to download")

    session = requests.Session()
    all_bodies = []
    all_ns = {
        "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    }

    # Batch download with VALUES
    total_batches = (len(uris) + VALUES_BATCH_SIZE - 1) // VALUES_BATCH_SIZE
    for i in range(0, len(uris), VALUES_BATCH_SIZE):
        batch = uris[i : i + VALUES_BATCH_SIZE]
        batch_no = i // VALUES_BATCH_SIZE
        print(f"[INFO] batch {batch_no + 1}/{total_batches} ({len(batch)} URIs)")

        try:
            rdf_xml = fetch_construct(session, batch)
        except requests.RequestException as e:
            print(f"[ERROR] batch {batch_no} failed: {e}")
            continue

        # Save individual batch RDF
        batch_file = RDF_DIR / f"civil_law_batch_{batch_no:03d}.rdf"
        batch_file.write_text(rdf_xml, encoding="utf-8")

        # Collect namespaces and body
        ns = collect_namespaces(rdf_xml)
        all_ns.update(ns)
        body = extract_rdf_body(rdf_xml)
        if body:
            all_bodies.append(body)

        time.sleep(SLEEP_SEC)

    print(f"\n[INFO] downloaded {len(all_bodies)} batches")

    # Build combined RDF/XML
    combined = build_combined_rdf(all_bodies, dict(all_ns))
    COMBINED_RDF.write_text(combined, encoding="utf-8")
    print(f"[DONE] combined RDF/XML: {COMBINED_RDF.resolve()} ({len(combined):,} bytes)")

    # Build OWL
    owl = build_owl(all_bodies, dict(all_ns))
    OWL_FILE.write_text(owl, encoding="utf-8")
    print(f"[DONE] OWL ontology: {OWL_FILE.resolve()} ({len(owl):,} bytes)")


if __name__ == "__main__":
    main()
