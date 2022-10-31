UPDATE users SET disabled = 0 WHERE disabled IS NULL;

UPDATE users SET substitute_account = 0 WHERE substitute_account IS NULL;

ALTER TABLE users MODIFY COLUMN disabled BOOLEAN NOT NULL DEFAULT 0,
                  MODIFY COLUMN substitute_account BOOLEAN NOT NULL DEFAULT 0;