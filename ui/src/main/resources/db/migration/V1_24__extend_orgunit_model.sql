ALTER TABLE orgunits ADD COLUMN location varchar(255) NULL;
ALTER TABLE orgunits_aud ADD COLUMN location varchar(255) NULL DEFAULT NULL;

ALTER TABLE orgunits ADD COLUMN url_address varchar(255) NULL;
ALTER TABLE orgunits_aud ADD COLUMN url_address varchar(255) NULL DEFAULT NULL;

ALTER TABLE orgunits ADD COLUMN opening_hours_phone TEXT NULL;
ALTER TABLE orgunits_aud ADD COLUMN opening_hours_phone TEXT NULL DEFAULT NULL;

ALTER TABLE orgunits ADD COLUMN email_notes TEXT NULL;
ALTER TABLE orgunits_aud ADD COLUMN email_notes TEXT NULL DEFAULT NULL;