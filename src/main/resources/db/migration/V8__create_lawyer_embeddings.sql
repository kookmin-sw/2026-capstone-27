-- Issue #50: 변호사 매칭 임베딩 — lawyer_embeddings (1:1 with lawyers)
--
-- 별도 테이블 분리 이유:
--  * lawyers 프로필 수정과 임베딩 갱신의 LOCK 경합 방지
--  * legal_chunks/legal_cases 와 동일 패턴 유지
--  * 1024차원 벡터 로드를 필요할 때만 JOIN 으로 수행
--  * embedding_model 교체 이력 추적 가능
--
-- 전제: V3 에서 CREATE EXTENSION IF NOT EXISTS vector 이미 수행됨.

CREATE TABLE IF NOT EXISTS lawyer_embeddings (
    lawyer_id        UUID            PRIMARY KEY
                                     REFERENCES lawyers(id) ON DELETE CASCADE,
    embedding        vector(1024)    NOT NULL,
    embedding_model  VARCHAR(64)     NOT NULL,
    source_hash      VARCHAR(64)     NOT NULL,
    source_text      TEXT,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- HNSW 인덱스 (legal_chunks 와 동일 파라미터)
CREATE INDEX IF NOT EXISTS idx_lawyer_embeddings_hnsw
    ON lawyer_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- source_hash 조회용 (재임베딩 판단 시 WHERE source_hash = ? 가능)
CREATE INDEX IF NOT EXISTS idx_lawyer_embeddings_source_hash
    ON lawyer_embeddings (source_hash);

-- updated_at 자동 갱신 (V3 에서 이미 정의된 touch_updated_at() 재사용)
CREATE TRIGGER trg_lawyer_embeddings_touch
    BEFORE UPDATE ON lawyer_embeddings
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

COMMENT ON TABLE lawyer_embeddings IS '변호사 프로필 임베딩 (Issue #50). 매칭 시 brief_embeddings 와 코사인 유사도 계산.';
COMMENT ON COLUMN lawyer_embeddings.embedding IS 'Cohere embed-v4.0 (1024-dim) 문서 임베딩.';
COMMENT ON COLUMN lawyer_embeddings.embedding_model IS '임베딩 생성 모델 ID. 모델 교체 시 재임베딩 판단용.';
COMMENT ON COLUMN lawyer_embeddings.source_hash IS 'SHA-256 of LawyerEmbeddingTextBuilder 출력. 프로필 변경 시 재임베딩 판단용.';
COMMENT ON COLUMN lawyer_embeddings.source_text IS '임베딩 입력 원문 (디버깅/감사). 옵션이며 용량 부담 시 후속 PR 에서 drop 고려.';
