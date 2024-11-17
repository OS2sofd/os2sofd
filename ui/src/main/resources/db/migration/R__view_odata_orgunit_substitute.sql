CREATE OR REPLACE VIEW view_odata_orgunit_substitute
AS
WITH RECURSIVE cte AS
(
   SELECT
      o.uuid,
      o.parent_uuid,
      CAST(o.uuid AS char(2000)) AS HIERARCHY
   FROM
      orgunits o
   WHERE
      o.parent_uuid IS NULL
   UNION ALL
   SELECT
      o.uuid,
      o.parent_uuid,
      concat(cte.hierarchy, '/', o.uuid) AS HIERARCHY
   FROM
      orgunits o
      INNER JOIN
         cte
         ON cte.uuid = o.parent_uuid
)
SELECT
   idx AS id,
   org_unit_uuid,
   substitute_uuid,
   substitute_name,
   substitute_context_name,
   substitute_context_identifier,
   TYPE,
   created,
   changed,
   inherited
FROM
   (
      SELECT
         ROW_NUMBER() OVER (ORDER BY id, TYPE) AS idx,
         org_unit_uuid,
         substitute_uuid,
         substitute_name,
         substitute_context_name,
         substitute_context_identifier,
         TYPE,
         created,
         changed,
         inherited
      FROM
         (
             # find all substitute_assignments and create a line for each
            SELECT
               a.id AS id,
               o.uuid AS org_unit_uuid,
               p.uuid AS substitute_uuid,
               CONCAT(p.firstname, ' ', p.surname) AS substitute_name,
               c.name AS substitute_context_name,
               c.identifier AS substitute_context_identifier,
               'Manager' AS 'type',
               a.created,
               a.changed,
               0 AS inherited
            FROM
               orgunits o
               INNER JOIN
                  view_orgunits_manager om
                  ON om.orgunit_uuid = o.uuid
               INNER JOIN
                  substitute_assignment a
                  ON a.person_uuid = om.manager_uuid
               INNER JOIN
                  persons p
                  ON p.uuid = a.substitute_uuid
               INNER JOIN
                  substitute_context c
                  ON a.substitute_context_id = c.id
               LEFT JOIN
                  substitute_assignment_orgunit sao
                  ON sao.substitute_assignment_id = a.id
			   INNER JOIN
                  affiliations prime_affiliation on prime_affiliation.person_uuid = p.uuid and prime_affiliation.prime = 1
            WHERE
               (
                  sao.orgunit_uuid = o.uuid
                  OR sao.orgunit_uuid IS NULL
               )
            UNION ALL
            # find all substitute_org_unit_assignments and create a line for each
            SELECT
               a.id AS id,
               cte.uuid AS org_unit_uuid,
               p.uuid AS substitute_uuid,
               CONCAT(p.firstname, ' ', p.surname) AS substitute_name,
               c.name AS substitute_context_name,
               c.identifier AS substitute_context_identifier,
               'OrgUnit' AS 'type',
               a.created,
               a.changed,
               CASE
                  WHEN
                     cte.uuid = o.uuid
                  THEN
                     0
                  ELSE
                     1
               END
               AS inhertied
            FROM
               orgunits o
               RIGHT JOIN
                  substitute_org_unit_assignment a
                  ON o.uuid = a.org_unit_uuid
               JOIN
                  substitute_context c
                  ON a.substitute_context_id = c.id
               INNER JOIN
                  cte
                  ON (cte.hierarchy LIKE concat('%', o.uuid, '%')
                  AND c.inherit_org_unit_assignments = 1)
                  OR cte.uuid = o.uuid
               JOIN
                  persons p
                  ON a.substitute_uuid = p.uuid
			   INNER JOIN
                  affiliations prime_affiliation on prime_affiliation.person_uuid = p.uuid and prime_affiliation.prime = 1
         )
         AS sub
   )
   AS sub2