CREATE OR REPLACE view `view_odata_affiliation_kle_primary`
AS
  SELECT `kle`.`code`          AS `code`,
         `kle`.`name`          AS `name`,
         `kle`.`active`        AS `active`,
         `affiliations`.`uuid` AS `affiliation_uuid`
  FROM   `kle`
  JOIN `affiliations_kle_primary` ON `affiliations_kle_primary`.`kle_value` = `kle`.`code`
  JOIN `affiliations` ON `affiliations`.`id` = `affiliations_kle_primary`.`affiliation_id`;