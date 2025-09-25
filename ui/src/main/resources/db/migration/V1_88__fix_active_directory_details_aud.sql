ALTER TABLE active_directory_details_aud MODIFY COLUMN password_locked BOOLEAN NULL;

ALTER TABLE security_log ADD COLUMN processed_time BIGINT NOT NULL DEFAULT 0;
