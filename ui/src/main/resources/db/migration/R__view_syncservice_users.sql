CREATE OR REPLACE VIEW view_syncservice_users AS
  SELECT
    p.uuid AS person_uuid,
    COALESCE(NULLIF(ad.kombit_uuid, ''), p.uuid) AS uuid,
    p.cpr,
    COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name,
    u.user_id,
    u.disabled,
    u.prime,
    u.user_type,
    e.email,
    ph.phone_number,
    prof.name AS profession_name,
    CASE
      WHEN TRIM(IFNULL(a.position_display_name, '')) <> '' THEN a.position_display_name
      WHEN prof.id IS NOT NULL AND setting.disable_professions IS NULL THEN prof.name ELSE a.position_name
    END AS position_name,
    a.pay_grade_text,
    COALESCE(a.start_date, '1979-05-21 00:00:00') AS start_date,
    COALESCE(workplace.orgunit_uuid, a.`alt_orgunit_uuid`, a.`orgunit_uuid`) AS `orgunit_uuid`,
    a.inherit_privileges,
    ad.upn,
    mitid.user_id AS nemlogin_user_uuid,
    kpa.kle_values AS kle_primary_values,
    ksa.kle_values AS kle_secondary_values
  FROM persons p
  JOIN persons_users pu ON pu.person_uuid = p.uuid
  JOIN users u ON u.id = pu.user_id
  LEFT JOIN affiliations a ON
    a.person_uuid = p.uuid AND (
        -- either user should match affiliation
        (u.user_type = 'ACTIVE_DIRECTORY' AND u.employee_id = a.employee_id)
        OR
        -- or the affiliation should not be mapped to another user
        (u.user_type IN ('ACTIVE_DIRECTORY','UNILOGIN') AND u.employee_id IS NULL AND (a.employee_id IS NULL OR a.employee_id NOT IN (
				SELECT distinct mu.employee_id
				FROM users mu
				INNER JOIN persons_users mpu ON mpu.user_id = mu.id
				WHERE mu.user_type = 'ACTIVE_DIRECTORY' AND mu.employee_id IS NOT NULL)
			)
        )
        OR
        -- or the usertype should be UNILOGIN
        u.user_type = 'UNILOGIN'
        -- or school_users
        OR
        (
        	u.user_type = 'ACTIVE_DIRECTORY_SCHOOL'
        	AND
        	(
        	    (
        	    	-- if no STIL-institution tags are set at all then skip the filter and include all affiliations
        	    	0 = (SELECT COUNT(*) FROM view_orgunit_tags ot WHERE ot.tag_name = 'STIL-institution' AND ot.tag_selected = 1)
        	    )
        	    OR
				(
					-- otherwise only include affiliations to orgunits tagged with STIL-institution (inheritance included)
		        	a.orgunit_uuid IN
		        	(
		        		SELECT orgunit_uuid
		        		FROM view_orgunit_tags ot
		        		WHERE
		        			ot.tag_name = 'STIL-institution'
		        			AND (ot.tag_selected = 1 OR ot.tag_inherited = 1)
		        	)
        	  	)
        	)
        )
    )
  LEFT JOIN professions prof on prof.id = a.profession_id
  LEFT JOIN (
    SELECT 1 AS disable_professions FROM settings WHERE setting_key = 'DISABLE_PROFESSIONS' AND setting_value = 'true'
  ) setting ON 1 = 1
  LEFT JOIN (
     SELECT orgunit_uuid, affiliation_id
     FROM affiliations_workplaces aw
     WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate()
  ) workplace ON workplace.affiliation_id = a.id
  LEFT JOIN orgunits o ON o.uuid = COALESCE(workplace.orgunit_uuid, a.`alt_orgunit_uuid`, a.`orgunit_uuid`)
  LEFT JOIN persons_phones pp ON pp.person_uuid = p.uuid
  LEFT JOIN phones ph ON ph.id = pp.phone_id
  -- join email
  LEFT JOIN (
	SELECT p.uuid as person_uuid, u.master_id, u.user_id AS email
	  FROM persons p
	  JOIN persons_users pu ON pu.person_uuid = p.uuid
	  JOIN users u ON u.id = pu.user_id
	  WHERE u.user_type IN ('EXCHANGE','SCHOOL_EMAIL')
	) e ON e.person_uuid = p.uuid AND ( (e.master_id = u.user_id AND u.user_type IN ('ACTIVE_DIRECTORY','ACTIVE_DIRECTORY_SCHOOL')) OR (e.master_id = u.uuid AND u.user_type = 'UNILOGIN'))
  -- join upn
  LEFT JOIN (
    SELECT user_id, upn, kombit_uuid
    FROM active_directory_details
  ) ad ON ad.user_id = u.id
  -- nemloginUserUuid
  LEFT JOIN (
    SELECT master_id, user_type, user_id FROM users WHERE disabled = 0
  ) mitid ON mitid.master_id = u.user_id AND mitid.user_type = 'MITID_ERHVERV'
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
    AND (u.user_type IN ('ACTIVE_DIRECTORY','ACTIVE_DIRECTORY_SCHOOL','UNILOGIN'))
    AND a.deleted = 0
    AND o.deleted = 0
    AND (a.stop_date IS NULL OR CAST(a.stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE));
