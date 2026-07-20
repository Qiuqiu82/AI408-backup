ALTER TABLE ai408_user
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER email;
