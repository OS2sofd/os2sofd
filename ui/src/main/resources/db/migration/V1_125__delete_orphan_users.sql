# delete orphan users
DELETE u
FROM users u
LEFT JOIN persons_users pu ON pu.user_id = u.id