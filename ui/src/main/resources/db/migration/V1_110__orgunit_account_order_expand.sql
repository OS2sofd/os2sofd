ALTER TABLE orgunit_account_order_type
    ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN deactivate_and_delete_rule varchar(64) NOT NULL DEFAULT 'KEEP_ALIVE';

ALTER TABLE affiliations
   ADD COLUMN deactivate_and_delete_rule varchar(64) NOT NULL DEFAULT 'KEEP_ALIVE';

ALTER TABLE affiliations_aud
   ADD COLUMN deactivate_and_delete_rule varchar(64) NULL;