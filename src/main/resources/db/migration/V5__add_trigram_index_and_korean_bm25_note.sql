-- Phase B-8c — 한국어 BM25 리콜 향상
--
-- 배경:
-- - Supabase는 pg_bigm, mecab_ko, textsearch_ko 등 한국어 형태소 분석 확장을
--   지원하지 않는다 (pg_available_extensions 조회 결과).
-- - 현재 content_tsv는 to_tsvector('simple', ...)로 구성되며 한국어에 대해
--   공백 기준 토큰화만 수행한다. "전세금이"와 "전세금"은 별도 토큰으로 저장된다.
-- - 따라서 조사/접미사가 붙은 실제 문서 토큰을 매칭하려면 BM25 쿼리 측에서
--   prefix 매칭(`:*`)을 사용하거나 pg_trgm의 부분일치로 보완해야 한다.
--
-- 이 마이그레이션은 **인덱스 레이어**만 처리한다. 쿼리 변환(`:*` 부여)은
-- Java 측 `PgLegalRetrievalService.buildKeywordTsQuery`에서 수행한다.
--
-- 1) pg_trgm GIN 인덱스
--    3-way 하이브리드의 trigram 경로가 seq scan으로 동작하던 문제를 해소한다.
--    Supabase 운영 DB에는 이미 수동 생성되어 있으며, 멱등성을 위해
--    IF NOT EXISTS로 선언한다.
CREATE INDEX IF NOT EXISTS idx_legal_chunks_content_trgm
    ON legal_chunks USING gin (content gin_trgm_ops);

-- 2) NOTE:
--    content_tsv 재생성을 수반하는 regconfig 변경(예: 커스텀 korean config)은
--    Supabase에서 확장 미지원으로 의미 있는 형태소 분석을 얻기 어렵고,
--    2025-04 기준 코퍼스 크기(1,193행)에서는 GIN 인덱스 재빌드 비용 대비
--    실익이 작다. 따라서 본 단계에서는 prefix 매칭 + trigram 2-way 보완
--    전략을 채택하며 regconfig 변경은 Phase C로 이연한다.
