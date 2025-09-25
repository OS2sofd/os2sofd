ALTER TABLE orgunits ADD COLUMN display_name varchar(255) NULL;
ALTER TABLE orgunits ADD COLUMN calculated_name varchar(255) NOT NULL DEFAULT '';
UPDATE orgunits SET calculated_name = name;

ALTER TABLE orgunits_aud ADD COLUMN display_name varchar(255) NULL DEFAULT NULL;
ALTER TABLE orgunits_aud ADD COLUMN calculated_name varchar(255) NULL DEFAULT NULL;

ALTER TABLE orgunit_change_queue ADD COLUMN display_name varchar(255) NULL;

ALTER TABLE orgunits ADD COLUMN cvr_name varchar(255) NULL;
ALTER TABLE orgunits_aud ADD COLUMN cvr_name varchar(255) NULL DEFAULT NULL;