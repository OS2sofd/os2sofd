CREATE OR REPLACE view `view_odata_affiliation_kle_primary`
AS
  SELECT `kle`.`code`                                 AS `code`,
         `kle`.`name`                                 AS `name`,
         `kle`.`active`                               AS `active`,
         `affiliations_kle_primary`.`affiliation_id`  AS `affiliation_id`
  FROM   `kle`
  JOIN `affiliations_kle_primary` ON `affiliations_kle_primary`.`kle_value` = `kle`.`code`;