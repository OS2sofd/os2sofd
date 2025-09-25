CREATE OR REPLACE VIEW view_syncservice_all_azure_users AS
  SELECT
    p.uuid AS person_uuid,
    u.master_id AS uuid,
    p.cpr,
    COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name,
    u.user_id,
    u.disabled,
    u.prime
  FROM persons p
  JOIN persons_users pu ON pu.person_uuid = p.uuid
  JOIN users u ON u.id = pu.user_id
  WHERE p.deleted = 0
    AND u.user_type = 'AZURE_AD';
