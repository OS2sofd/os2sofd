CREATE OR REPLACE view `view_odata_orgunit_email`
AS
  SELECT `emails`.`id`                    AS `id`,
         `emails`.`prime`                 AS `prime`,
         `emails`.`email`                 AS `email`,
         `emails`.`master`                AS `master`,
         `emails`.`master_id`             AS `master_id`,
         `orgunits_emails`.`orgunit_uuid` AS `org_unit_uuid`
  FROM   `emails`
  JOIN `orgunits_emails` ON `orgunits_emails`.`email_id` = `emails`.`id`;
