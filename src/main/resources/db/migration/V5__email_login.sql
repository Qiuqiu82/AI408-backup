ALTER TABLE ai408_user
    MODIFY mobile VARCHAR(20) NULL,
    ADD COLUMN email VARCHAR(120) NULL AFTER mobile,
    ADD UNIQUE KEY uk_ai408_user_email (email);

CREATE TABLE IF NOT EXISTS ai408_login_code (
    id VARCHAR(64) NOT NULL,
    email VARCHAR(120) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    scene VARCHAR(20) NOT NULL,
    expires_at DATETIME NOT NULL,
    last_sent_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    used TINYINT(1) NOT NULL DEFAULT 0,
    used_at DATETIME NULL,
    device_id VARCHAR(100) NULL,
    client_type VARCHAR(20) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai408_login_code_email_scene_used_created (email, scene, used, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
