CREATE OR REPLACE view `view_odata_orgunit_manager`
AS
SELECT ROW_NUMBER() OVER (ORDER BY om.orgunit_uuid)             AS id,
       om.orgunit_uuid						                    AS org_unit_uuid,
       om.manager_uuid						                    AS person_uuid,
       om.inherited							                    AS inherited,
       om.name                                                  AS name
FROM   orgunits_manager om
WHERE om.manager_uuid is not null