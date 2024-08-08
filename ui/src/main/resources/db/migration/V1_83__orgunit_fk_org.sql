ALTER TABLE orgunits ADD COLUMN do_not_transfer_to_fk_org BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE orgunits_aud ADD COLUMN do_not_transfer_to_fk_org BOOLEAN NULL;
