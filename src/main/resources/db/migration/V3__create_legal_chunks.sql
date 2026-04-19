-- Phase A: legal_chunks 스토리지 PG 이관 (MongoDB 제거)
-- NOTE: embedding vector column은 Phase B (V4__add_embedding_column.sql)에서 추가

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS legal_chunks (
    id               BIGSERIAL       PRIMARY KEY,
    law_id           VARCHAR(64)     NOT NULL,
    law_name         VARCHAR(255)    NOT NULL,
    article_no       VARCHAR(32)     NOT NULL,
    chunk_index      SMALLINT        NOT NULL DEFAULT 0,
    article_title    VARCHAR(255),
    content          TEXT            NOT NULL,
    effective_date   DATE,
    abolition_date   DATE,
    source_url       VARCHAR(512),
    category_ids     TEXT[],
    lod_uri          VARCHAR(512),
    legislation_terms TEXT[],
    content_tsv      tsvector GENERATED ALWAYS AS (
                         to_tsvector('simple',
                             coalesce(law_name, '')      || ' ' ||
                             coalesce(article_no, '')    || ' ' ||
                             coalesce(article_title, '') || ' ' ||
                             content)
                     ) STORED,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- 조문 단위 단일 chunk 일 때 (chunk_index=0)는 물론, 다수 chunk일 때도 안전
CREATE UNIQUE INDEX IF NOT EXISTS uq_legal_chunks_active_article
    ON legal_chunks (law_id, article_no, chunk_index)
    WHERE abolition_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_legal_chunks_tsv
    ON legal_chunks USING GIN (content_tsv);

CREATE INDEX IF NOT EXISTS idx_legal_chunks_law_id
    ON legal_chunks (law_id);

CREATE INDEX IF NOT EXISTS idx_legal_chunks_active
    ON legal_chunks (law_id)
    WHERE abolition_date IS NULL;

CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_legal_chunks_touch
    BEFORE UPDATE ON legal_chunks
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
