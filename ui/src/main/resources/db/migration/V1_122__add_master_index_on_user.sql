# delete duplicate master + master_id rows, keeping the initial record
DELETE u
FROM users u
         JOIN (
    SELECT id
    FROM (
             SELECT
                 id,
                 ROW_NUMBER() OVER (PARTITION BY master, master_id ORDER BY id) AS rn
             FROM users
         ) ranked
    WHERE rn > 1
) dup ON u.id = dup.id;

# introduce new unique indexe on master and master_id
ALTER TABLE users ADD UNIQUE INDEX ux_users__master_master_id (master, master_id);