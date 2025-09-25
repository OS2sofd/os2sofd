CREATE OR REPLACE view `view_odata_person_child`
AS
SELECT `persons_children`.`id`           AS `ref_id`,
     `persons_children`.`cpr`            AS `cpr`,
     `persons_children`.`name`           AS `name`,
     `persons_children`.`person_uuid`    AS `person_uuid`
FROM `persons_children`