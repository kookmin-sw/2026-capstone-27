-- Phase C-3: 판례(legal_cases) 스토리지 스키마 도입
--
-- 설계 메모:
--  * 판례 1건 = 1 row. 법령(legal_chunks)이 조문 단위로 분할되는 것과 대비.
--    판례는 판시사항·판결요지·본문이 유기적으로 연결돼 검색 시 통째로 유사도를 재는
--    편이 자연스럽다(대법원 통합조회 제공 단위와 일치).
--  * 임베딩: Cohere embed-v4.0(1024-dim), HNSW cosine 인덱스. legal_chunks와 동일.
--    본문이 임베딩 입력 한계(~512~2048 토큰)를 넘길 수 있어 실제 인제스트 단계(C-4)에서는
--    "판시사항 + 판결요지 (+ 본문 앞부분)" 조합을 임베딩 문자열로 사용한다.
--  * 전문검색(BM25): tsvector GENERATED 컬럼 — 사건명/판시사항/판결요지/본문 concat.
--  * 참조 법령: text[]로 보관 (예: ARRAY['민법 제312조의2', '주택임대차보호법 제3조의2']).
--    조문 단위 검색을 위해 별도 링크 테이블은 C-4에서 필요 시 도입.
--  * 도메인 카테고리: legal_chunks.category_ids와 동일한 온톨로지 카테고리 ID 사용.
--
-- 전제: V3 에서 CREATE EXTENSION IF NOT EXISTS vector, pg_trgm 이미 수행됨.
--       V3 에서 touch_updated_at() 함수도 이미 정의됨.

CREATE TABLE IF NOT EXISTS legal_cases (
    id                  BIGSERIAL       PRIMARY KEY,

    -- 식별: 한국 법원 사건번호 체계는 "연도 + 사건부호 + 일련번호" (예: "2020다12345").
    -- 단, 심급이 여러 개일 경우 같은 사건번호로 복수 판결이 나올 수 있어
    -- (case_no, court, decision_date) 조합을 논리적 식별키로 쓴다.
    case_no             VARCHAR(64)     NOT NULL,
    court               VARCHAR(100)    NOT NULL,
    case_name           VARCHAR(500),
    decision_date       DATE            NOT NULL,

    -- 분류
    case_type           VARCHAR(32)     NOT NULL,       -- 민사/형사/가사/행정/특허 등
    judgment_type       VARCHAR(32),                    -- 판결/결정/명령
    disposition         VARCHAR(200),                   -- 주문 요약 (상고기각, 파기환송 등)

    -- 본문
    headnote            TEXT,                           -- 판시사항
    holding             TEXT,                           -- 판결요지
    reasoning           TEXT,                           -- 판결이유 (법원의 판단)
    full_text           TEXT,                           -- 원문 전체 (있는 경우)

    -- 참조
    cited_articles      TEXT[],                         -- 참조 법령·조문 (예: {"민법 제312조의2"})
    cited_cases         TEXT[],                         -- 참조 판례 사건번호
    category_ids        TEXT[],                         -- 온톨로지 카테고리 ID (legal_chunks와 동일 체계)

    -- 출처
    source              VARCHAR(32)     NOT NULL        -- 'law.go.kr', 'scourt.go.kr' 등
                                        DEFAULT 'law.go.kr',
    source_url          VARCHAR(512),
    source_id           VARCHAR(128),                   -- 법제처 판례일련번호 또는 외부 ID

    -- BM25 용 GENERATED tsvector
    content_tsv         tsvector GENERATED ALWAYS AS (
                            to_tsvector('simple',
                                coalesce(case_name, '')  || ' ' ||
                                coalesce(case_no, '')    || ' ' ||
                                coalesce(headnote, '')   || ' ' ||
                                coalesce(holding, '')    || ' ' ||
                                coalesce(reasoning, ''))
                        ) STORED,

    -- 벡터
    embedding           vector(1024),
    embedding_model     VARCHAR(64),

    -- 감사
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- 자연키 중복 방지 (같은 법원의 같은 사건번호·선고일 중복 인제스트 차단)
CREATE UNIQUE INDEX IF NOT EXISTS uq_legal_cases_natural_key
    ON legal_cases (case_no, court, decision_date);

-- BM25 GIN
CREATE INDEX IF NOT EXISTS idx_legal_cases_tsv
    ON legal_cases USING GIN (content_tsv);

-- 카테고리 배열 GIN (legal_chunks.category_ids와 동일 패턴)
CREATE INDEX IF NOT EXISTS idx_legal_cases_category_ids
    ON legal_cases USING GIN (category_ids);

-- 참조 법령 배열 GIN (조문 기반 판례 역조회용)
CREATE INDEX IF NOT EXISTS idx_legal_cases_cited_articles
    ON legal_cases USING GIN (cited_articles);

-- case_type 단독 필터용
CREATE INDEX IF NOT EXISTS idx_legal_cases_case_type
    ON legal_cases (case_type);

-- 선고일 범위 필터용 (최근 판례 가중치 적용 대비)
CREATE INDEX IF NOT EXISTS idx_legal_cases_decision_date
    ON legal_cases (decision_date DESC);

-- pg_trgm — 본문 유사도 (3-way 하이브리드 trigram 파트)
-- headnote + holding 합쳐 트라이그램 운영하는 방식도 가능하나,
-- 한국어 판례는 요지 중심 검색이므로 holding에 집중 (legal_chunks는 content 사용).
CREATE INDEX IF NOT EXISTS idx_legal_cases_holding_trgm
    ON legal_cases USING GIN (holding gin_trgm_ops);

-- HNSW — pgvector cosine 유사도
CREATE INDEX IF NOT EXISTS idx_legal_cases_embedding_hnsw
    ON legal_cases
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64)
    WHERE embedding IS NOT NULL;

-- 임베딩 미완료 행 백필 추적
CREATE INDEX IF NOT EXISTS idx_legal_cases_embedding_null
    ON legal_cases ((embedding IS NULL))
    WHERE embedding IS NULL;

-- updated_at 자동 갱신 (V3에서 정의한 함수 재사용)
-- DROP → CREATE 패턴으로 재실행 안전성 확보.
DROP TRIGGER IF EXISTS trg_legal_cases_touch ON legal_cases;
CREATE TRIGGER trg_legal_cases_touch
    BEFORE UPDATE ON legal_cases
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

COMMENT ON TABLE legal_cases IS
    'Phase C-3: 대법원/하급심 판례. 판례 1건 = 1 row. 임베딩은 headnote+holding 조합 텍스트에 대해 Cohere embed-v4.0 적용.';
COMMENT ON COLUMN legal_cases.case_no IS '사건번호 (예: 2020다12345). 단일 사건에 복수 심급 있을 수 있어 court·decision_date와 결합해 식별.';
COMMENT ON COLUMN legal_cases.headnote IS '판시사항 (법원이 제시한 쟁점·요점).';
COMMENT ON COLUMN legal_cases.holding IS '판결요지 (법원의 법리 결론).';
COMMENT ON COLUMN legal_cases.reasoning IS '판결이유 (판단 근거).';
COMMENT ON COLUMN legal_cases.cited_articles IS '참조 법령·조문 텍스트 배열 (예: {"민법 제312조의2","주택임대차보호법 제3조의2"}).';
COMMENT ON COLUMN legal_cases.category_ids IS 'legal_chunks.category_ids와 동일한 온톨로지 카테고리 ID 체계.';
COMMENT ON COLUMN legal_cases.embedding IS 'Cohere embed-v4.0 (1024-dim). C-4 인제스트 파이프라인이 채움.';
