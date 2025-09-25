ALTER TABLE persons ADD COLUMN has_updated_authorization_code BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE persons_aud ADD COLUMN has_updated_authorization_code BOOLEAN NULL;

UPDATE persons SET has_updated_authorization_code = 1;