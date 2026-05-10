-- Issue #48 (Issue B): per-level classification lock 도입.
--   primary_field_locked boolean 한 개로 분류 차단하던 방식을 폐기하고
--   Consultation.userDomains / userSubDomains / userTags 비어있음 여부로
--   per-level 판정하므로 이 컬럼은 더 이상 필요 없다.
--
-- 롤백:
--   ALTER TABLE consultations ADD COLUMN primary_field_locked BOOLEAN NOT NULL DEFAULT FALSE;
--   (백업한 값으로 UPDATE 필요)
ALTER TABLE consultations DROP COLUMN IF EXISTS primary_field_locked;
