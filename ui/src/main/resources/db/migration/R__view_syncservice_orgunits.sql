-- one-shots, to cleanup old views
DROP VIEW IF EXISTS view_orgunits_kle_primary;
DROP VIEW IF EXISTS view_orgunits_kle_secondary;
DROP VIEW IF EXISTS view_orgunits;

CREATE OR REPLACE VIEW subview_syncservice_orgunits_kle_primary AS
  SELECT kp.orgunit_uuid,  GROUP_CONCAT(kp.kle_value SEPARATOR ',') AS kle_values
    FROM orgunits_kle_primary kp
    GROUP BY kp.orgunit_uuid;

CREATE OR REPLACE VIEW subview_syncservice_orgunits_kle_secondary AS
  SELECT ks.orgunit_uuid, GROUP_CONCAT(ks.kle_value SEPARATOR ',') AS kle_values
    FROM orgunits_kle_secondary ks
    GROUP BY ks.orgunit_uuid;

CREATE OR REPLACE VIEW view_syncservice_orgunits AS
  SELECT o.uuid,
    o.name,
    o.parent_uuid,
    o.inherit_kle,
    e.kombit_uuid AS manager_uuid,
    kpa.kle_values AS kle_primary_values,
    ksa.kle_values AS kle_secondary_values
  FROM orgunits o
  LEFT JOIN orgunits_manager m ON m.orgunit_uuid = o.uuid
  -- join manager ad account
  LEFT JOIN (
	SELECT pu.person_uuid, ad.kombit_uuid
	  FROM persons_users pu
	  JOIN users u ON u.id = pu.user_id
	  JOIN active_directory_details ad ON ad.user_id = u.id
	  WHERE u.user_type = 'ACTIVE_DIRECTORY' AND u.prime = 1
	) e ON e.person_uuid = m.manager_uuid
  LEFT JOIN subview_syncservice_orgunits_kle_primary kpa ON kpa.orgunit_uuid = o.uuid
  LEFT JOIN subview_syncservice_orgunits_kle_secondary ksa ON ksa.orgunit_uuid = o.uuid
  INNER JOIN view_adm_organisation vao ON vao.id = o.belongs_to
  WHERE o.deleted = 0;