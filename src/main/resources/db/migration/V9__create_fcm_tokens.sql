-- Issue #70: FCM 푸시 알림 — fcm_tokens
--
-- 디바이스 토큰 저장 테이블. 한 사용자가 여러 디바이스 (PC + 모바일 + 태블릿) 에서
-- 알림을 받을 수 있도록 1:N 관계로 설계.
--
-- 운영 시나리오:
--  * 사용자가 기기에서 알림 권한 허용 → FE 가 FCM 에서 token 발급 → BE 에 등록
--  * 사용자가 알림 끔 → FE 가 token 삭제 요청 → BE 에서 row 제거
--  * 토큰 만료/회전 → 새 token 등록 (UNIQUE 제약으로 중복 방지)
--  * FCM 발송 시 send 결과의 invalid token error → 해당 row 자동 삭제
--
-- device_type: 'WEB' | 'ANDROID' | 'IOS' (선택). 디바이스별 통계·디버깅용.

CREATE TABLE IF NOT EXISTS fcm_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token        TEXT         NOT NULL UNIQUE,
    device_type  VARCHAR(20),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

-- 사용자별 토큰 조회용 (알림 발송 시 user_id → tokens 룩업)
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON fcm_tokens(user_id);

COMMENT ON TABLE  fcm_tokens IS 'Firebase Cloud Messaging 디바이스 토큰. user 1:N device.';
COMMENT ON COLUMN fcm_tokens.token       IS 'FCM 디바이스 토큰. 갱신 시 upsert 필요.';
COMMENT ON COLUMN fcm_tokens.device_type IS '''WEB'' | ''ANDROID'' | ''IOS'' (선택).';
