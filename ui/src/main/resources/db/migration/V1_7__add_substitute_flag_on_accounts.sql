ALTER TABLE users ADD substitute_account BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE users_aud ADD substitute_account BOOLEAN NULL;