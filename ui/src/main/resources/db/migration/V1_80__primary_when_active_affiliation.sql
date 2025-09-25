ALTER TABLE affiliations ADD COLUMN use_as_primary_when_active BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE affiliations_aud ADD COLUMN use_as_primary_when_active BOOLEAN NULL;