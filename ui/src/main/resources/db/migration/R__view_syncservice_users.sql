-- one-shots, to cleanup old views
DROP VIEW IF EXISTS view_affiliations_primary_kle;
DROP VIEW IF EXISTS view_affiliations_secondary_kle;
DROP VIEW IF EXISTS view_ad_users;
DROP VIEW IF EXISTS subview_syncservice_person_prime_email;
DROP VIEW IF EXISTS subview_syncservice_affiliations_primary_kle;
DROP VIEW IF EXISTS subview_syncservice_affiliations_secondary_kle;

CREATE OR REPLACE VIEW view_syncservice_users AS
  SELECT p.uuid,
    p.cpr,
    COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name,
    u.user_id,
    u.disabled,
    u.prime,
    u.user_type,
    e.email,
    ph.phone_number,
    a.position_name,
    a.orgunit_uuid,
    a.inherit_privileges,
    ad.upn,
    kpa.kle_values AS kle_primary_values,
    ksa.kle_values AS kle_secondary_values
  FROM persons p
  JOIN persons_users pu ON pu.person_uuid = p.uuid
  JOIN users u ON u.id = pu.user_id
  LEFT JOIN affiliations a ON
    a.person_uuid = p.uuid AND (
        -- either user should match affiliation
        u.employee_id = a.employee_id
        OR
        -- or the affiliation should not be mapped to another user
        (u.employee_id IS NULL AND (a.employee_id IS NULL OR a.employee_id NOT IN (
				SELECT distinct mu.employee_id
				FROM users mu
				INNER JOIN persons_users mpu ON mpu.user_id = mu.id AND mpu.person_uuid = p.uuid
				WHERE mu.user_type = 'ACTIVE_DIRECTORY' AND mu.employee_id IS NOT NULL)
			)
        )
    )
  LEFT JOIN orgunits o ON o.uuid = a.orgunit_uuid
  LEFT JOIN persons_phones pp ON pp.person_uuid = p.uuid
  LEFT JOIN phones ph ON ph.id = pp.phone_id
  -- join email
  LEFT JOIN (
	SELECT p.uuid as person_uuid, u.master_id, u.user_id AS email
	  FROM persons p
	  JOIN persons_users pu ON pu.person_uuid = p.uuid
	  JOIN users u ON u.id = pu.user_id
	  WHERE u.user_type = 'EXCHANGE'
	) e ON e.person_uuid = p.uuid AND e.master_id = u.user_id
  -- join upn
  LEFT JOIN (
    SELECT user_id, upn
    FROM active_directory_details
  ) ad ON ad.user_id = u.id
  -- join primary kle
  LEFT JOIN (
	  SELECT kp.affiliation_id, GROUP_CONCAT(kp.kle_value SEPARATOR ',') AS kle_values
		FROM affiliations_kle_primary kp
		GROUP BY kp.affiliation_id
  ) kpa ON kpa.affiliation_id = a.id
  -- join secondary kle
  LEFT JOIN (
	  SELECT ks.affiliation_id, GROUP_CONCAT(ks.kle_value SEPARATOR ',') AS kle_values
		FROM affiliations_kle_secondary ks
		GROUP BY ks.affiliation_id
  ) ksa ON ksa.affiliation_id = a.id
  INNER JOIN view_adm_organisation vao ON vao.id = o.belongs_to
  WHERE p.deleted = 0
    AND p.force_stop = 0
    AND (ph.prime IS NULL OR ph.prime = 1)
    AND (u.user_type = 'ACTIVE_DIRECTORY' OR u.user_type = 'UNILOGIN')
    AND a.deleted = 0
    AND o.deleted = 0
    AND (a.stop_date IS NULL OR CAST(a.stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE));
