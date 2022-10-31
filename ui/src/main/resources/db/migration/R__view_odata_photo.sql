CREATE OR REPLACE view `view_odata_photo`
AS
  SELECT `photos`.`id`                  AS `id`,
         `photos`.`person_uuid`         AS `person_uuid`,
         `photos`.`last_changed`        AS `last_changed`,
         `photos`.`data`                AS `data`,
         `photos`.`checksum`            AS `checksum`,
         `photos`.`format`         		AS `format`
  FROM   `photos`;