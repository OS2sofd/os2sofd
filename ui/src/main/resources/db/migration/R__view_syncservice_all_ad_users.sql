CREATE OR REPLACE VIEW view_syncservice_all_ad_users AS
SELECT
    p.uuid AS person_uuid,
    COALESCE(NULLIF(ad.kombit_uuid, ''), p.uuid) AS uuid,
    p.cpr,
    COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name,
    u.user_id,
    u.disabled,
    CASE 
        WHEN ad.account_expire_date IS NOT NULL AND ad.account_expire_date <= CURDATE() THEN 1 
        ELSE 0 
    END AS expired,
    CASE 
        WHEN ad.password_expire_date IS NOT NULL AND ad.password_expire_date > '1970-01-01' 
        THEN ad.password_expire_date 
        ELSE NULL 
    END AS password_expire_date,
    u.prime,
    e.user_id AS email,
    ad.upn,
    o.name AS primary_orgunit_name,
    u.local_extensions,
    CASE WHEN p.person_type = 'ROBOT' THEN 1 ELSE 0 END AS robot
FROM users u
INNER JOIN persons_users pu ON pu.user_id = u.id
INNER JOIN persons p ON p.uuid = pu.person_uuid AND p.deleted = 0
LEFT JOIN active_directory_details ad ON ad.user_id = u.id
LEFT JOIN (
    SELECT pu2.person_uuid, u2.master_id, u2.user_id
    FROM persons_users pu2
    INNER JOIN users u2 ON u2.id = pu2.user_id AND u2.user_type = 'EXCHANGE'
) e ON e.person_uuid = p.uuid AND e.master_id = u.user_id
LEFT JOIN (
    SELECT a.person_uuid, ou.name
    FROM affiliations a
    INNER JOIN orgunits ou ON ou.uuid = a.orgunit_uuid
    WHERE a.prime = 1
) o ON o.person_uuid = p.uuid
WHERE u.user_type = 'ACTIVE_DIRECTORY';