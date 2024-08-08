ALTER TABLE persons ADD COLUMN updated_from_cpr BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE persons_aud ADD COLUMN updated_from_cpr BOOLEAN NULL;

UPDATE persons SET updated_from_cpr = 1 WHERE created < (DATE_SUB(curdate(), INTERVAL 2 MONTH));