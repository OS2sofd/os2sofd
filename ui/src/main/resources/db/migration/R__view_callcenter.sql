DROP VIEW IF EXISTS subview_callcenter_orgunit_prime_email;

CREATE OR REPLACE VIEW subview_callcenter_prime_ad_user AS
(
	SELECT
		pu.person_uuid
        ,u.user_id
        FROM persons_users pu
        INNER JOIN users u ON u.id = pu.user_id AND u.user_type = 'ACTIVE_DIRECTORY' AND u.prime = 1
);

CREATE OR REPLACE VIEW subview_callcenter_orgunit_prime_post AS
(
	SELECT
		op.orgunit_uuid
        ,CONCAT(p.street, ' ', p.postal_code ,' ', p.city) AS 'address'
	FROM orgunits_posts op
	INNER JOIN posts p ON p.id = op.post_id and p.prime=1
);

CREATE OR REPLACE VIEW subview_callcenter_person_prime_email AS
(
	SELECT p.uuid AS person_uuid, u.id, u.prime, u.user_id AS email, u.master, u.master_id
	  FROM persons p
	  JOIN persons_users pu ON pu.person_uuid = p.uuid
	  JOIN users u ON u.id = pu.user_id
	  WHERE u.user_type = 'EXCHANGE' AND u.prime = 1
);

CREATE OR REPLACE VIEW subview_callcenter_prime_person_phone AS
(
	SELECT
		pp.person_uuid,
		p.id,
		p.prime,
		p.type_prime,
		IF(p.visibility = 'VISIBLE', p.phone_number, NULL) AS 'phone_number',
		p.phone_type,
		p.master,
		p.master_id,
		p.notes,
		p.visibility,
		p.function_type_id
	FROM persons_phones pp
	INNER JOIN phones p ON p.id = pp.phone_id AND p.prime = 1
);

CREATE OR REPLACE VIEW subview_callcenter_person_phone_numbers AS
(
    SELECT
        pp.person_uuid,
        GROUP_CONCAT(IF(p.visibility = 'VISIBLE', p.phone_number, NULL) SEPARATOR ',') AS phone_numbers
    FROM phones p
    INNER JOIN persons_phones pp ON pp.phone_id = p.id
    WHERE p.phone_type IN ('MOBILE', 'IP', 'LANDLINE') AND p.prime=0
    GROUP BY pp.person_uuid
);

CREATE OR REPLACE VIEW subview_callcenter_prime_orgunit_phone AS
(
	SELECT
		oup.orgunit_uuid,
		p.id,
		p.prime,
		p.type_prime,
		IF(p.visibility = 'VISIBLE', p.phone_number, NULL) AS 'phone_number',
		p.phone_type,
		p.master,
		p.master_id,
		p.notes,
		p.visibility,
		p.function_type_id
	FROM orgunits_phones oup
	INNER JOIN phones p ON p.id = oup.phone_id AND p.prime = 1
);

CREATE OR REPLACE VIEW subview_callcenter_orgunit_phone_numbers AS
(
    SELECT
        ou.orgunit_uuid,
        GROUP_CONCAT(IF(p.visibility = 'VISIBLE', p.phone_number, NULL) SEPARATOR ',') AS phone_numbers
    FROM phones p
    INNER JOIN orgunits_phones ou ON ou.phone_id = p.id
    WHERE p.phone_type IN ('MOBILE', 'IP', 'LANDLINE') AND p.prime=0
    GROUP BY ou.orgunit_uuid
);

CREATE OR REPLACE VIEW subview_callcenter_employees AS
(
	SELECT DISTINCT
		'Employee'				AS 'type',
		af.uuid                 AS 'uuid',
		IFNULL(p.chosen_name,CONCAT(p.firstname,' ',p.surname)) AS 'name',
		ou.name                 AS 'org_unit',
		padu.user_id			AS 'user_id',
		p.key_words				AS 'keywords',
        ou.opening_hours		AS 'opening_hours',
		ppp.phone_number		AS 'phone',
		ppn.phone_numbers       AS 'phone_numbers',
		ppe.email				AS 'email',
		oupp.address			AS 'address',
		af.position_name		AS 'position_name',
		p.notes                 AS 'notes',
		IFNULL(mp.chosen_name,CONCAT(mp.firstname,' ',mp.surname)) AS 'manager_name',
		mppp.phone_number		AS 'manager_phone',
		pop.phone_number		AS 'org_unit_phone'
	FROM
		persons p
		INNER JOIN affiliations af ON
			af.person_uuid = p.uuid
			AND af.deleted = 0
			AND (af.start_date IS NULL OR CAST(af.start_date AS DATE) <= CAST(CURRENT_TIMESTAMP AS DATE))
			AND (af.stop_date IS NULL OR CAST(af.stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))
		LEFT JOIN (
        	SELECT orgunit_uuid, affiliation_id
            FROM affiliations_workplaces aw
            WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate()
        ) workplace ON workplace.affiliation_id = af.id
        INNER JOIN orgunits ou ON ou.uuid = COALESCE(workplace.orgunit_uuid, af.alt_orgunit_uuid, af.orgunit_uuid) and ou.deleted = 0
        LEFT JOIN subview_callcenter_prime_orgunit_phone pop ON pop.orgunit_uuid = ou.uuid
		LEFT JOIN subview_callcenter_orgunit_prime_post oupp ON oupp.orgunit_uuid = ou.uuid
		LEFT JOIN subview_callcenter_prime_ad_user padu ON padu.person_uuid = p.uuid
		LEFT JOIN subview_callcenter_prime_person_phone ppp ON ppp.person_uuid = p.uuid
		LEFT JOIN subview_callcenter_person_prime_email ppe ON ppe.person_uuid = p.uuid
		LEFT JOIN subview_callcenter_person_phone_numbers ppn ON ppn.person_uuid = p.uuid
		LEFT JOIN orgunits_manager ouman ON ouman.orgunit_uuid = ou.uuid
        LEFT JOIN persons mp ON mp.uuid = ouman.manager_uuid
        LEFT JOIN subview_callcenter_prime_person_phone mppp ON mppp.person_uuid = mp.uuid
);

CREATE OR REPLACE VIEW subview_callcenter_orgunits AS
(
	SELECT DISTINCT
		'OrgUnit'			AS 'type',
		ou.uuid             AS 'uuid',
		ou.name 			AS 'name',
		ou.name 			AS 'org_unit',
		NULL 				AS 'user_id',
        ou.key_words		AS 'keywords',
        ou.opening_hours	AS 'opening_hours',
		pop.phone_number	AS 'phone',
		oupn.phone_numbers  AS 'phone_numbers',
		ou.email			AS 'email',
		oupp.address        AS 'address',
		NULL				AS 'position_name',
		ou.notes            AS 'notes',
		IFNULL(mp.chosen_name,CONCAT(mp.firstname,' ',mp.surname)) AS 'manager_name',
		mppp.phone_number   AS 'manager_phone',
		pop.phone_number	AS 'org_unit_phone'
	FROM
		orgunits ou
        LEFT JOIN subview_callcenter_prime_orgunit_phone pop ON pop.orgunit_uuid = ou.uuid
        LEFT JOIN subview_callcenter_orgunit_prime_post oupp ON oupp.orgunit_uuid = ou.uuid
        LEFT JOIN subview_callcenter_orgunit_phone_numbers oupn ON oupn.orgunit_uuid = ou.uuid
        LEFT JOIN orgunits_manager ouman ON ouman.orgunit_uuid = ou.uuid
        LEFT JOIN persons mp ON mp.uuid = ouman.manager_uuid
        LEFT JOIN subview_callcenter_prime_person_phone mppp ON mppp.person_uuid = mp.uuid
	WHERE ou.deleted = 0
);

CREATE OR REPLACE VIEW view_callcenter AS
	SELECT * FROM subview_callcenter_orgunits
    UNION ALL
    SELECT * FROM subview_callcenter_employees;

