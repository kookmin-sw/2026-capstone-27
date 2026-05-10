# 민법 LOD 온톨로지 (메타 참조용)

## 개요

국가법령정보센터(law.go.kr)의 Linked Open Data 저장소에서 수집한 **민법 관련 법령 온톨로지**입니다. 출처: `https://github.com/capstoneSHIELD/SHIELD_FE/tree/a7cc5a9/lod_law_downloads`.

## 용도

**RAG 학습/검색에는 사용하지 않습니다**. 그 이유는 다음과 같습니다.

- 본 파일들은 법령 본문을 포함하지 않고, 190개 민법 관련 법령 인스턴스의 **메타 그래프(URI 참조)**만 제공
- `hasArticleCategory`, `hasLegislationTermCategory` 리소스에는 레이블/정의가 없어 의미 해석 불가 (외부 SPARQL endpoint 질의 필요)
- RAG의 본문 검색 요건은 law.go.kr OpenAPI로 수집한 `src/main/resources/seed/civil-law.json`이 이미 충족

## 파일 구성

| 파일 | 크기 | 내용 |
|---|---|---|
| `civil_law_legislation.csv` | 23KB | 민법 관련 190개 법령 메타데이터 테이블 |
| `civil_law_legislation.json` | 45KB | 위 내용의 JSON 표현 |
| `civil_law_ontology.owl` | 6MB | OWL 클래스 정의 + 190개 법령 인스턴스 (`KoreanLegislationNorms` 107건, `KoreanLegislation` 83건) |
| `civil_law_ontology.rdf` | 6MB | RDF/XML 형식의 동일 인스턴스 그래프 |

### 민법 LOD 식별자

- URI: `http://lod.law.go.kr/resource/LSI265307`
- 클래스: `KoreanLegislationNorms`
- 연결된 리소스: 2,118 legislationTermType, 1,192 articleType, 36 additionalRulesCategory
- articleType URI 규칙: `articleType_LSI265307_{조번호:4자리}_{분기:2자리}` (분기 0=본조, 2 이상=조의N)

### law.go.kr 다른 식별자와의 대응

| 출처 | 민법 식별자 |
|---|---|
| law.go.kr OpenAPI (`lawService`) | `001706` (법령ID), `284415` (MST) |
| law.go.kr LOD | `LSI265307` |

## 향후 활용 후보 과제

- 민법과 190개 관련 법령 간 **관계 그래프 시각화**
- SPARQL endpoint를 통한 **법률 용어 정의 자동 보강**
- 조문 간 상호 참조 네트워크 구축
