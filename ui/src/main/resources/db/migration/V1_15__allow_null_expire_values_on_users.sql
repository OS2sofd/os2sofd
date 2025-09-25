ALTER TABLE users MODIFY COLUMN password_expire_date DATE NULL;
ALTER TABLE users MODIFY COLUMN account_expire_date DATE NULL;
UPDATE users SET password_expire_date = NULL;
UPDATE users SET account_expire_date = NULL;