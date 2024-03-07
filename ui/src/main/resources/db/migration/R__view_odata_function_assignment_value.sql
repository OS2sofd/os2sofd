 CREATE OR REPLACE view `view_odata_function_assignment_value`
 AS
  SELECT
    fv.id
    ,fv.function_assignment_id
    ,f.name
    ,f.type
    ,CASE
      WHEN f.type = 'LIST' THEN li.text
      WHEN f.type = 'ORG' THEN org.orgunits
      WHEN f.type = 'FREETEXT' THEN fv.text
      WHEN f.type = 'EMPLOYEE' THEN ifnull(p.chosen_name,concat(p.firstname, ' ',p.surname))
      WHEN f.type = 'FOLLOW_UP_DATE' THEN fv.date
      ELSE ''
	END as value
  FROM fh_facet_value fv
  INNER JOIN fh_facet f on f.id = fv.facet_id
  LEFT JOIN fh_list_item li on li.id = fv.list_item_id
  LEFT JOIN (
    SELECT
      fvo.facet_value_id
      ,group_concat(o.name ORDER BY o.name SEPARATOR '; ') as orgunits
    FROM fh_facet_value_orgunit fvo
    LEFT JOIN orgunits o on o.uuid = fvo.orgunit_uuid
  ) org on org.facet_value_id = fv.id
  LEFT JOIN affiliations a ON a.id = fv.affiliation_id
  LEFT JOIN persons p on p.uuid = a.person_uuid;