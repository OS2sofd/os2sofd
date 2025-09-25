CREATE OR REPLACE view `view_odata_affiliation_kle_secondary`
AS
  SELECT `kle`.`code`                                  AS `code`,
         `kle`.`name`                                  AS `name`,
         `kle`.`active`                                AS `active`,
         `affiliations_kle_secondary`.`affiliation_id` AS `affiliation_id`
  FROM   `kle`
  JOIN `affiliations_kle_secondary` ON `affiliations_kle_secondary`.`kle_value` = `kle`.`code`;