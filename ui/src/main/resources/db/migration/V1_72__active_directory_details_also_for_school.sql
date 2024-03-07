
-- add userType for debugging purposes now that we support adm/school ADs at the same time
ALTER TABLE active_directory_details ADD COLUMN user_type VARCHAR(36) NOT NULL DEFAULT 'ACTIVE_DIRECTORY';
ALTER TABLE active_directory_details_aud ADD COLUMN user_type VARCHAR(36) NULL;

-- we get duplicates sometimes in production, this will block it
ALTER TABLE active_directory_details ADD CONSTRAINT uc_add_userid UNIQUE (user_id);
