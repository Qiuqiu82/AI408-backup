CREATE TABLE IF NOT EXISTS ai408_user (
    id VARCHAR(64) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL,
    refresh_token_hash VARCHAR(128) NULL,
    refresh_token_expires_at DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai408_user_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai408_question (
    id VARCHAR(64) NOT NULL,
    question_code VARCHAR(64) NULL,
    subject_code VARCHAR(20) NOT NULL,
    subject_name VARCHAR(50) NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    stem LONGTEXT NULL,
    options_json LONGTEXT NULL,
    answer_json LONGTEXT NULL,
    analysis LONGTEXT NULL,
    note LONGTEXT NULL,
    tags_json LONGTEXT NULL,
    new_type TINYINT(1) NULL,
    steps_json LONGTEXT NULL,
    difficulty INT NULL,
    sort_no INT NULL,
    source VARCHAR(50) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai408_question_code (question_code),
    KEY idx_ai408_question_subject_sort (subject_code, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai408_user_question_state (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    question_id VARCHAR(64) NOT NULL,
    question_status VARCHAR(20) NOT NULL,
    favorite_importance INT NOT NULL DEFAULT 0,
    note LONGTEXT NULL,
    in_wrong_book TINYINT(1) NOT NULL DEFAULT 0,
    last_wrong_at VARCHAR(32) NULL,
    last_favorite_at VARCHAR(32) NULL,
    correct_count INT NOT NULL DEFAULT 0,
    wrong_count INT NOT NULL DEFAULT 0,
    essay_done TINYINT(1) NOT NULL DEFAULT 0,
    selected_json LONGTEXT NULL,
    step_status_json LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai408_state_user_question (user_id, question_id),
    KEY idx_ai408_state_user_wrong (user_id, in_wrong_book),
    KEY idx_ai408_state_user_favorite (user_id, favorite_importance)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai408_import_job (
    job_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    import_type VARCHAR(20) NULL,
    total_count INT NULL,
    success_count INT NULL,
    failed_count INT NULL,
    error_file_url VARCHAR(255) NULL,
    message LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai408_practice_session (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL,
    total_count INT NOT NULL,
    answered_count INT NOT NULL,
    current_question_id VARCHAR(64) NULL,
    question_ids_json LONGTEXT NULL,
    review_id VARCHAR(64) NULL,
    duration_seconds INT NULL,
    correct_count INT NULL,
    wrong_count INT NULL,
    started_at VARCHAR(32) NULL,
    finished_at VARCHAR(32) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai408_session_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai408_session_question (
    id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    question_id VARCHAR(64) NOT NULL,
    order_no INT NOT NULL,
    question_status VARCHAR(20) NOT NULL,
    new_type TINYINT(1) NOT NULL,
    answer_json LONGTEXT NULL,
    is_correct TINYINT(1) NULL,
    correct_answer_json LONGTEXT NULL,
    analysis LONGTEXT NULL,
    step_status_json LONGTEXT NULL,
    elapsed_seconds INT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai408_session_question (session_id, question_id),
    KEY idx_ai408_session_question_session_order (session_id, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
