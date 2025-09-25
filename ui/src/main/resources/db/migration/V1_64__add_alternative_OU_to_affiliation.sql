ALTER TABLE affiliations ADD alt_orgunit_uuid VARCHAR(36) NULL;
ALTER TABLE affiliations ADD CONSTRAINT fk_affiliations_alt_orgunit_uuid FOREIGN KEY (alt_orgunit_uuid) REFERENCES orgunits(uuid);

ALTER TABLE affiliations_aud ADD alt_orgunit_uuid VARCHAR(36) NULL;