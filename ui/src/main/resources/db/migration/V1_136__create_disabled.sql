ALTER TABLE supported_user_types ADD COLUMN create_as_disabled BOOLEAN NOT NULL DEFAULT 0 AFTER days_before_to_create;
ALTER TABLE supported_user_types ADD COLUMN days_before_to_reactivate BOOLEAN NOT NULL DEFAULT 0 AFTER create_as_disabled;
