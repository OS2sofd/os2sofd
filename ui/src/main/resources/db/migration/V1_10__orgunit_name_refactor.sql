ALTER TABLE orgunits ADD COLUMN source_name varchar(255) NULL;
ALTER TABLE orgunits_aud ADD COLUMN source_name varchar(255) NULL DEFAULT NULL;

UPDATE orgunits SET source_name = name;
UPDATE orgunits SET name = calculated_name;

ALTER TABLE orgunits DROP COLUMN calculated_name;