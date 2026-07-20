ALTER TABLE ai408_practice_session
    ADD COLUMN scope_type VARCHAR(32) NULL AFTER subject_code,
    ADD COLUMN scope_key VARCHAR(100) NULL AFTER scope_type,
    ADD COLUMN scope_name VARCHAR(255) NULL AFTER scope_key;

CREATE INDEX idx_ai408_session_scope
    ON ai408_practice_session (scope_type, scope_key);
