ALTER TABLE orgunits ADD COLUMN affiliation_duration_rule VARCHAR(32) NOT NULL DEFAULT 'INHERIT';
ALTER TABLE orgunits ADD COLUMN affiliation_max_days INT NULL;
ALTER TABLE orgunits_aud ADD COLUMN affiliation_duration_rule VARCHAR(32) NULL;
ALTER TABLE orgunits_aud ADD COLUMN affiliation_max_days INT NULL;
