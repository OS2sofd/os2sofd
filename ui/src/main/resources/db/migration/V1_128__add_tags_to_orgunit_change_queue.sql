ALTER TABLE orgunit_change_queue ADD COLUMN tag_id bigint NULL;
ALTER TABLE orgunit_change_queue ADD COLUMN tag_value varchar(255) NULL;