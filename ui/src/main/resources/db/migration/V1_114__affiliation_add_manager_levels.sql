ALTER TABLE affiliations ADD COLUMN superior_level VARCHAR(64) NULL;
ALTER TABLE affiliations ADD COLUMN subordinate_level VARCHAR(64) NULL;
ALTER TABLE affiliations_aud ADD COLUMN superior_level VARCHAR(64) NULL;
ALTER TABLE affiliations_aud ADD COLUMN subordinate_level VARCHAR(64) NULL;
