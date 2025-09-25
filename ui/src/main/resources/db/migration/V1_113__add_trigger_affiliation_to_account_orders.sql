ALTER TABLE account_orders ADD COLUMN trigger_affiliation_id BIGINT,
ADD CONSTRAINT fk_trigger_affiliation FOREIGN KEY (trigger_affiliation_id) REFERENCES affiliations (id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE account_orders_aud ADD COLUMN trigger_affiliation_id BIGINT;
