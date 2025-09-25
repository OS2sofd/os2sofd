ALTER TABLE client 
	ADD COLUMN application_identifier VARCHAR(255) NULL,
	ADD COLUMN newest_version VARCHAR(255) NULL,
	ADD COLUMN minimum_version VARCHAR(255) NULL,
	ADD COLUMN version_status VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE client_aud
	ADD COLUMN application_identifier VARCHAR(255) NULL,
	ADD COLUMN newest_version VARCHAR(255) NULL,
	ADD COLUMN minimum_version VARCHAR(255) NULL,
	ADD COLUMN version_status VARCHAR(255) NULL;
