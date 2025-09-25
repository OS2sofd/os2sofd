ALTER TABLE persons ADD COLUMN authorization_code VARCHAR(255) NULL;
ALTER TABLE persons_aud ADD COLUMN authorization_code VARCHAR(255) NULL;

ALTER TABLE active_directory_details ADD COLUMN upn VARCHAR(255) NULL;
ALTER TABLE active_directory_details_aud ADD COLUMN upn VARCHAR(255) NULL;
