CREATE OR REPLACE VIEW view_orgunit_tags AS
WITH RECURSIVE cte as
(
    SELECT
        o.uuid as orgunit_uuid
        ,o.parent_uuid
        ,o.name
        ,t.id as tag_id
        ,t.value as tag_name
        ,CASE WHEN ot.id IS NULL THEN 0 ELSE 1 END as tag_selected
        ,0 as tag_inherited
        ,ot.custom_value as tag_value
    FROM
        orgunits o
    LEFT JOIN tags t ON TRUE
    LEFT JOIN orgunits_tags ot ON ot.tag_id = t.id AND ot.orgunit_uuid = o.uuid
    WHERE
        o.parent_uuid IS NULL
        AND o.deleted = 0

    UNION ALL

    SELECT
        o.uuid as orgunit_uuid
        ,o.parent_uuid
        ,o.name
        ,parent.tag_id as tag_id
        ,parent.tag_name as tag_name
        ,CASE WHEN ot.id IS NULL THEN 0 ELSE 1 END as tag_selected
        ,CASE WHEN ot.id IS NULL AND parent.tag_selected = 1 OR parent.tag_inherited = 1 THEN 1 ELSE 0 END as tag_inherited
        ,IFNULL(ot.custom_value, parent.tag_value) as tag_value
    FROM
        orgunits o
    INNER JOIN cte parent on parent.orgunit_uuid = o.parent_uuid
    LEFT JOIN orgunits_tags ot ON ot.tag_id = parent.tag_id AND ot.orgunit_uuid = o.uuid
    WHERE
        o.deleted = 0
)
SELECT
    orgunit_uuid
    ,name
    ,tag_id
    ,tag_name
    ,tag_selected
    ,tag_inherited
    ,tag_value
FROM
    cte