ALTER TABLE orgunit_account_order_type_position
    ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT true;
