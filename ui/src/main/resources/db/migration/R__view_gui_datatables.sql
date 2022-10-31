DROP VIEW IF EXISTS persons_grid;
DROP VIEW IF EXISTS persons_deleted_grid;

CREATE OR REPLACE VIEW subview_datatables_affiliations AS SELECT
     CASE WHEN TRIM(IFNULL(position_display_name,'')) <> '' THEN position_display_name ELSE position_name END AS position_name,
     person_uuid,
     orgunit_uuid
   FROM affiliations
   WHERE prime = 1;

CREATE OR REPLACE VIEW view_datatables_persons AS SELECT
    p.uuid,
    COALESCE(p.chosen_name, CONCAT(p.firstname, ' ', p.surname)) AS name,
    CONCAT(a.position_name, ' i ', o.name) AS affiliation,
    o.uuid AS orgunit_uuid,
    phs.phone_number,
    IF (l.start_date IS NOT NULL AND l.start_date < CURRENT_TIMESTAMP, 1, 0) AS `leave`,
    p.force_stop,
    p.disable_account_orders,
    GROUP_CONCAT(usrs.user_id ORDER BY usrs.prime DESC, usrs.user_id) AS user_ids,
    STR_TO_DATE(LEFT(p.cpr, 6), '%d%m%y') IS NULL AS fictive_cpr
  FROM persons p
  LEFT JOIN (
    SELECT pu.person_uuid, u.user_id, u.prime
      FROM persons_users pu
      INNER JOIN users u ON u.id = pu.user_id AND u.user_type = 'ACTIVE_DIRECTORY'
    ) usrs ON usrs.person_uuid = p.uuid
  LEFT JOIN (
    SELECT pp.person_uuid, p.phone_number
      FROM persons_phones pp
      INNER JOIN phones p ON p.id = pp.phone_id AND p.prime = 1
    ) phs ON phs.person_uuid = p.uuid
  LEFT JOIN subview_datatables_affiliations a ON p.uuid = a.person_uuid
  LEFT JOIN persons_leave l ON p.leave_id = l.id
  LEFT JOIN orgunits o ON o.uuid = a.orgunit_uuid
  WHERE p.deleted = 0
  GROUP BY p.uuid;

CREATE OR REPLACE VIEW view_datatables_persons_deleted AS SELECT
    p.uuid,
    CONCAT(p.firstname, ' ', p.surname) AS name,
    NULL AS affiliation,
    NULL AS orgunit_uuid,
    NULL AS phone_number,
    0 AS `leave`,
    0 AS force_stop,
    0 AS disable_account_orders,
    NULL AS user_ids,
    0 AS fictive_cpr
  FROM persons p
  WHERE p.deleted = 1;
