CREATE OR REPLACE view `view_odata_orgunit_manager`
AS
  SELECT `orgunits_manager`.`id`           AS `id`,
         `orgunits_manager`.`orgunit_uuid` AS `org_unit_uuid`,
         `orgunits_manager`.`manager_uuid` AS `person_uuid`,
         `orgunits_manager`.`inherited`    AS `inherited`,
         `orgunits_manager`.`name`         AS `name`
  FROM   `orgunits_manager`;
