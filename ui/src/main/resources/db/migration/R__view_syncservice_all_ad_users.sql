CREATE OR REPLACE VIEW view_syncservice_all_ad_users AS
  SELECT
    p.uuid AS person_uuid,
    COALESCE(NULLIF(ad.kombit_uuid, ''), p.uuid) AS uuid,
    p.cpr,
    COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name,
    u.user_id,
    u.disabled,
    IF(ad.account_expire_date IS NOT NULL AND ad.account_expire_date <= CURDATE(), 1, 0) AS expired,
    u.prime,
    e.email,
    ad.upn,
    o.name AS primary_orgunit_name,
    u.local_extensions
  FROM persons p
  JOIN persons_users pu ON pu.person_uuid = p.uuid
  JOIN users u ON u.id = pu.user_id
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
    SELECT user_id, upn, kombit_uuid, account_expire_date
    FROM active_directory_details
  ) ad ON ad.user_id = u.id
  -- join primary orgunit name
  LEFT JOIN (
    SELECT a.person_uuid, ou.name
    FROM affiliations a
    JOIN orgunits ou ON ou.uuid = a.orgunit_uuid
    WHERE a.prime = 1
  ) o ON o.person_uuid = p.uuid
  WHERE p.deleted = 0
    AND p.force_stop = 0
    AND u.user_type = 'ACTIVE_DIRECTORY';
