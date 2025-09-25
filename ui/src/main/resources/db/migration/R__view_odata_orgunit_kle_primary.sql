CREATE OR REPLACE view `view_odata_orgunit_kle_primary`
AS
  SELECT `kle`.`code`                          AS `code`,
         `kle`.`name`                          AS `name`,
         `kle`.`active`                        AS `active`,
         `orgunits_kle_primary`.`orgunit_uuid` AS `org_unit_uuid`
  FROM   `kle`
  JOIN `orgunits_kle_primary` ON `orgunits_kle_primary`.`kle_value` = `kle`.`code`;
