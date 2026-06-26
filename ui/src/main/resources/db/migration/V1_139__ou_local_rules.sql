ALTER TABLE orgunit_account_order_type ADD COLUMN local_rules BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE orgunit_account_order_type ADD COLUMN days_before_to_create BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orgunit_account_order_type ADD COLUMN days_before_to_reactivate BIGINT NOT NULL DEFAULT 0;
