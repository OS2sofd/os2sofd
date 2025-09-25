ALTER TABLE persons ADD COLUMN disable_account_orders_disable BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE persons ADD COLUMN disable_account_orders_delete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE persons RENAME COLUMN disable_account_orders TO disable_account_orders_create;

ALTER TABLE persons_aud ADD COLUMN disable_account_orders_disable BOOLEAN NULL;
ALTER TABLE persons_aud ADD COLUMN disable_account_orders_delete BOOLEAN NULL;
ALTER TABLE persons_aud RENAME COLUMN disable_account_orders TO disable_account_orders_create;
