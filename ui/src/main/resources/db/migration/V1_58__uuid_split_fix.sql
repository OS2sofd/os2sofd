ALTER TABLE fk_org_uuids ADD COLUMN user_id VARCHAR(255) NOT NULL;
ALTER TABLE fk_org_uuids DROP COLUMN user_uuid;
