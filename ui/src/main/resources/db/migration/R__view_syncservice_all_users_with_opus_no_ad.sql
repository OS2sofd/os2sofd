CREATE OR REPLACE VIEW view_syncservice_all_users_with_opus_no_ad AS
SELECT
    p.uuid AS person_uuid,
    p.cpr,
    COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name,
    a.employee_id,
    p.deleted
FROM affiliations a
JOIN persons p on a.person_uuid=p.uuid
LEFT JOIN (
    SELECT u.user_type, u.id, u.disabled, pu.person_uuid
    FROM persons_users pu
    JOIN users u ON u.id = pu.user_id AND u.disabled IS FALSE and (u.user_type = 'ACTIVE_DIRECTORY' OR u.user_type = 'ACTIVE_DIRECTORY_SCHOOL')
) ad ON ad.person_uuid = p.uuid
WHERE
    a.master='OPUS' AND
    a.employee_id IS NOT NULL AND
    ad.id IS NULL AND
    (a.start_date IS NULL OR a.start_date <= (CURDATE() + INTERVAL 5 DAY)) AND
    (a.stop_date IS NULL OR a.stop_date > CURDATE());
