ALTER TABLE supported_user_types ADD COLUMN days_to_cleanup BIGINT NOT NULL DEFAULT 0 AFTER days_to_deactivate;
ALTER TABLE supported_user_types ADD COLUMN cleanup_enabled BOOLEAN NOT NULL DEFAULT 0 AFTER deactivate_enabled;
