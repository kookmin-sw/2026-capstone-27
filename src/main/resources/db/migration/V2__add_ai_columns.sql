-- Sprint 2: AI 시스템 연동을 위한 Consultation 테이블 컬럼 추가
-- 적용 대상: consultations 테이블

-- xAI Responses API의 response.id (Stateful 대화 연결용)
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS last_response_id TEXT;

-- primary_field_locked 플래그 (P0-V: 사용자 수정 시 LLM override 방지)
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS primary_field_locked BOOLEAN NOT NULL DEFAULT false;
