CREATE OR REPLACE view `view_odata_orgunit_kle_tertiary`
AS
  SELECT `kle`.`code`                            AS `code`,
         `kle`.`name`                            AS `name`,
         `kle`.`active`                          AS `active`,
         `orgunits_kle_tertiary`.`orgunit_uuid` AS `org_unit_uuid`
  FROM   `kle`
  JOIN `orgunits_kle_tertiary` ON `orgunits_kle_tertiary`.`kle_value` = `kle`.`code`;
