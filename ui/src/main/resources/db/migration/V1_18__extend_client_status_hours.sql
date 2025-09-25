ALTER TABLE client ADD COLUMN warning_state_hours INT NOT NULL DEFAULT 48;
ALTER TABLE client ADD COLUMN error_state_hours INT NOT NULL DEFAULT 168;

ALTER TABLE client_aud ADD COLUMN warning_state_hours INT NULL;
ALTER TABLE client_aud ADD COLUMN error_state_hours INT NULL;
