CREATE OR REPLACE view `view_odata_person_authorization_code`
AS
  SELECT `authorization_code`.`id`						AS `id`,
         `authorization_code`.`prime`               	AS `prime`,
         `authorization_code`.`code`          			AS `code`,
         `authorization_code`.`name`        			AS `name`,
         `persons_authorization_codes`.`person_uuid`	AS `person_uuid`
  FROM   `authorization_code`
  JOIN `persons_authorization_codes` ON `persons_authorization_codes`.`authorization_code_id` = `authorization_code`.`id`;