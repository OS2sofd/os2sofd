CREATE OR REPLACE view `view_odata_orgunit_kle_secondary`
AS
  SELECT `kle`.`code`                            AS `code`,
         `kle`.`name`                            AS `name`,
         `kle`.`active`                          AS `active`,
         `orgunits_kle_secondary`.`orgunit_uuid` AS `org_unit_uuid`
  FROM   `kle`
  JOIN `orgunits_kle_secondary` ON `orgunits_kle_secondary`.`kle_value` = `kle`.`code`;
