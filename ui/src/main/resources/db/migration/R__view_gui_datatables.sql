DROP VIEW IF EXISTS persons_grid;
DROP VIEW IF EXISTS persons_deleted_grid;

CREATE OR REPLACE VIEW subview_datatables_affiliations AS SELECT
     CASE WHEN TRIM(IFNULL(position_display_name,'')) <> '' THEN position_display_name ELSE position_name END AS position_name,
     person_uuid,
     COALESCE(workplace.orgunit_uuid, a.`alt_orgunit_uuid`, a.`orgunit_uuid`) AS orgunit_uuid
     FROM affiliations a
     LEFT JOIN (
        SELECT aw.orgunit_uuid, aw.affiliation_id
        FROM affiliations_workplaces aw
        WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate() LIMIT 1
     ) workplace ON workplace.affiliation_id = a.id
   WHERE prime = 1;

CREATE OR REPLACE VIEW view_datatables_persons AS SELECT
    p.uuid,
    COALESCE(p.chosen_name, CONCAT(p.firstname, ' ', p.surname)) AS name,
    CONCAT(a.position_name, ' i ', o.name) AS affiliation,
    o.uuid AS orgunit_uuid,
    phs.phone_number,
    p.cpr,
    IF (l.start_date IS NOT NULL AND l.start_date < CURRENT_TIMESTAMP, 1, 0) AS `leave`,
    p.force_stop,
    p.disable_account_orders,
    GROUP_CONCAT(usrs.user_id ORDER BY usrs.prime DESC, usrs.user_id) AS user_ids,
    STR_TO_DATE(LEFT(p.cpr, 6), '%d%m%y') IS NULL AS fictive_cpr,
    p.dead AS dead,
    p.disenfranchised AS disenfranchised
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
    p.cpr,
    0 AS `leave`,
    0 AS force_stop,
    0 AS disable_account_orders,
    GROUP_CONCAT(usrs.user_id ORDER BY usrs.prime DESC, usrs.user_id) AS user_ids,
    0 AS fictive_cpr,
    p.dead AS dead,
    p.disenfranchised AS disenfranchised
  FROM persons p
  LEFT JOIN (
    SELECT pu.person_uuid, u.user_id, u.prime
      FROM persons_users pu
      INNER JOIN users u ON u.id = pu.user_id AND u.user_type = 'ACTIVE_DIRECTORY'
    ) usrs ON usrs.person_uuid = p.uuid
  WHERE p.deleted = 1
  GROUP BY p.uuid;

CREATE OR REPLACE VIEW view_datatables_students AS SELECT
    s.uuid,
    s.name,
    s.disabled,
    s.username,
    GROUP_CONCAT(sin.institution_number) AS institution_numbers,
    GROUP_CONCAT(scn.class) AS classes
  FROM student s
  LEFT JOIN student_institution_numbers sin ON s.id = sin.student_id
  LEFT JOIN student_class_names scn ON s.id = scn.student_id
  GROUP BY s.id;