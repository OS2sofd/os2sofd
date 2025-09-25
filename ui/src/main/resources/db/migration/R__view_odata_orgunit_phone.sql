CREATE OR REPLACE view `view_odata_orgunit_phone`
AS
  SELECT `phones`.`id`                    AS `id`,
         `phones`.`prime`                 AS `prime`,
         `phones`.`type_prime`            AS `type_prime`,
         `phones`.`phone_number`          AS `phone_number`,
         `phones`.`phone_type`            AS `phone_type`,
         `phones`.`master`                AS `master`,
         `phones`.`master_id`             AS `master_id`,
         `phones`.`notes`                 AS `notes`,
         `phones`.`visibility`            AS `visibility`,
         `function_types`.`name`          AS `function_type`,
         `orgunits_phones`.`orgunit_uuid` AS `org_unit_uuid`
  FROM   `phones`
  JOIN `orgunits_phones` ON `orgunits_phones`.`phone_id` = `phones`.`id`
  LEFT JOIN `function_types` ON `function_types`.`id` = `phones`.`function_type_id`;
