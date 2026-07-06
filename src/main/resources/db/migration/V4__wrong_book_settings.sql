ALTER TABLE ai408_user
    ADD COLUMN wrong_book_auto_remove_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN wrong_book_auto_remove_threshold INT NOT NULL DEFAULT 1;

ALTER TABLE ai408_user_question_state
    ADD COLUMN wrong_book_resolve_streak INT NOT NULL DEFAULT 0;
