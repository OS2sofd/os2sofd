CREATE OR REPLACE view `view_odata_orgunit_manager`
AS
SELECT om.id							       AS id,
       om.orgunit_uuid						AS org_unit_uuid,
       om.manager_uuid						AS person_uuid,
       om.inherited							AS inherited,
       ifnull(m.chosen_name,concat(m.firstname,' ',m.surname))	AS name
FROM   orgunits_manager om
INNER JOIN persons m on m.uuid = om.manager_uuid;