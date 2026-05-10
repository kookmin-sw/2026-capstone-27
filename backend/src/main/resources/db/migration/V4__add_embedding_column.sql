-- Phase B-1: legal_chunks 임베딩 벡터 컬럼 추가
-- 모델: Cohere embed-v4.0 (1024차원, 다국어)
-- 인덱스: HNSW (cosine 거리)

ALTER TABLE legal_chunks
    ADD COLUMN IF NOT EXISTS embedding vector(1024);

ALTER TABLE legal_chunks
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(64);

-- HNSW 인덱스: cosine 유사도 기반
-- m=16 (기본값), ef_construction=64 (품질과 빌드시간 균형)
-- 임베딩이 없는 행은 인덱스에서 제외하여 빌드 성능 확보
CREATE INDEX IF NOT EXISTS idx_legal_chunks_embedding_hnsw
    ON legal_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64)
    WHERE embedding IS NOT NULL;

-- 임베딩 유무 빠른 필터용 (인제스트 파이프라인의 백필 추적에 사용)
CREATE INDEX IF NOT EXISTS idx_legal_chunks_embedding_null
    ON legal_chunks ((embedding IS NULL))
    WHERE embedding IS NULL;

COMMENT ON COLUMN legal_chunks.embedding IS 'Cohere embed-v4.0 (1024-dim) vector. NULL until B-2 인제스트 파이프라인이 채움.';
COMMENT ON COLUMN legal_chunks.embedding_model IS '임베딩 생성에 사용된 모델 ID (예: embed-v4.0). 재임베딩 필요성 판단용.';
