CREATE OR REPLACE view `view_odata_orgunit`
AS
  SELECT `orgunits`.`uuid`             AS `uuid`,
         `orgunits`.`master`           AS `master`,
         `orgunits`.`master_id`        AS `master_id`,
         `orgunits`.`deleted`          AS `deleted`,
         `orgunits`.`created`          AS `created`,
         `orgunits`.`last_changed`     AS `last_changed`,
         `orgunits`.`parent_uuid`      AS `parent_uuid`,
         `orgunits`.`shortname`        AS `shortname`,
         `orgunits`.`name`             AS `name`,
         `orgunits`.`cvr`              AS `cvr`,
         `orgunits`.`cvr_name`         AS `cvr_name`,
         `orgunits`.`ean`              AS `ean`,
         `orgunits`.`senr`             AS `senr`,
         `orgunits`.`pnr`              AS `pnr`,
         `orgunits`.`cost_bearer`      AS `cost_bearer`,
         `orgunits`.`org_type`         AS `org_type`,
         `orgunits`.`org_type_id`      AS `org_type_id`,
         `orgunits`.`local_extensions` AS `local_extensions`,
         `orgunits`.`key_words`        AS `key_words`,
         `orgunits`.`opening_hours`    AS `opening_hours`,
         `orgunits`.`notes`            AS `notes`,
         `orgunits`.`display_name`     AS `display_name`,
         `orgunits`.`source_name`      AS `source_name`
  FROM   `orgunits`
  INNER JOIN `view_adm_organisation` ON `view_adm_organisation`.`id` = `orgunits`.`belongs_to`;