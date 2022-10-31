CREATE OR REPLACE view `view_odata_person_substitute`
AS
  SELECT `substitute_assignment`.`id`                AS `id`,
         `substitute_assignment`.`person_uuid`       AS `person_uuid`,
         `substitute_assignment`.`substitute_uuid`   AS `substitute_uuid`,
         `substitute_context`.`name`                  AS `substitute_context_name`,
         `substitute_context`.`identifier`            AS `substitute_context_identifier`,
         `substitute_context`.`supports_constraints`  AS `substitute_context_supports_constraints`,
         `substitute_assignment`.`created`            AS `created`,
         `substitute_assignment`.`changed`            AS `changed`
  FROM   `substitute_assignment` 
  JOIN `substitute_context` ON `substitute_assignment`.`substitute_context_id` = `substitute_context`.`id`;