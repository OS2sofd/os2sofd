DROP TABLE orgunits_emails_aud;
DROP TABLE orgunits_emails;
DROP TABLE emails_aud;
DROP TABLE emails;

ALTER TABLE orgunits ADD COLUMN email VARCHAR(255) NULL;
ALTER TABLE orgunits_aud ADD COLUMN email VARCHAR(255) NULL;