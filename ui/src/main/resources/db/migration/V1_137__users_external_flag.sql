ALTER TABLE active_directory_details ADD COLUMN external BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE active_directory_details_aud ADD COLUMN external BOOLEAN NULL;

ALTER TABLE account_orders ADD COLUMN external BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE account_orders_aud ADD COLUMN external BOOLEAN NULL;
