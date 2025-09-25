SELECT CONCAT(
 'ALTER TABLE `reserved_usernames` DROP FOREIGN KEY `',
 constraint_name,
 '`'
) INTO @sqlst
 FROM information_schema.KEY_COLUMN_USAGE
 WHERE table_name = 'reserved_usernames'
  AND referenced_table_name='persons'
  AND referenced_column_name='uuid' LIMIT 1;

SELECT @sqlst;

PREPARE stmt FROM @sqlst;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sqlst = NULL;

ALTER TABLE reserved_usernames
 ADD CONSTRAINT
 FOREIGN KEY (person_uuid)
 REFERENCES persons(uuid)
 ON DELETE CASCADE;