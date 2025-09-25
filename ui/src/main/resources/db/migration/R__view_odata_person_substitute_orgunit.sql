CREATE OR REPLACE VIEW `view_odata_person_substitute_orgunit`
AS
    SELECT
       `substitute_assignment_orgunit`.`id`                          AS `id`,
       `substitute_assignment_orgunit`.`substitute_assignment_id`    AS `substitute_assignment_id`,
       `substitute_assignment_orgunit`.`orgunit_uuid`                AS `orgunit_uuid`
    FROM `substitute_assignment_orgunit`;
