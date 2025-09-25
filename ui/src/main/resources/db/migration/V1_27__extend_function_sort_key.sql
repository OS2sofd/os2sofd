ALTER TABLE fh_function ADD COLUMN sort_key BIGINT NOT NULL DEFAULT 0;
UPDATE fh_function SET sort_key = id;