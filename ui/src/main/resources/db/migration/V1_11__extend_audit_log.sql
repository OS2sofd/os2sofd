ALTER TABLE audit_log ADD COLUMN entity_name VARCHAR(255) NULL AFTER entity_id;
ALTER TABLE audit_log ADD COLUMN message TEXT NULL;
ALTER TABLE audit_log MODIFY entity_id VARCHAR(64) NULL;
ALTER TABLE audit_log DROP COLUMN username;