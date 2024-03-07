CREATE OR REPLACE view `view_odata_orgunit_tag`
AS
  SELECT `orgunits_tags`.`id`             AS `ref_id`,
         `tags`.`value`                   AS `tag`,
         `tags`.`description`             AS `description`,
         `orgunits_tags`.`custom_value`   AS `custom_value`,
         `orgunits_tags`.`orgunit_uuid`   AS `org_unit_uuid`
  FROM   `tags`
  JOIN `orgunits_tags` ON `orgunits_tags`.`tag_id` = `tags`.`id`;
