ALTER TABLE supported_user_types ADD COLUMN deactivate_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE supported_user_types ADD COLUMN create_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE supported_user_types ADD COLUMN delete_enabled BOOLEAN DEFAULT FALSE;
UPDATE supported_user_types SET deactivate_enabled = (days_to_deactivate > 0);
UPDATE supported_user_types SET create_enabled = (days_before_to_create > 0);
UPDATE supported_user_types SET delete_enabled = (days_to_delete > 0);



