ALTER TABLE account_orders ADD COLUMN manual BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE account_orders_aud ADD COLUMN manual BOOLEAN NULL;
UPDATE account_orders set manual = TRUE WHERE requester_uuid IS NOT NULL;
