ALTER TABLE ai408_question
    ADD COLUMN stem_image_url VARCHAR(500) NULL AFTER stem;

ALTER TABLE ai408_session_question
    ADD COLUMN stem_image_url VARCHAR(500) NULL AFTER new_type;
