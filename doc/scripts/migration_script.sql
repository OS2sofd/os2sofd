# Find all users that might have problems after migratino
SELECT * FROM (
SELECT CONCAT(per.firstname, ' ', per.surname) AS name, us.user_id AS user_id, us.prime AS prime FROM persons per
  JOIN persons_users perus ON perus.person_uuid = per.uuid
  JOIN users us ON us.id = perus.user_id
  WHERE us.user_type = 'ACTIVE_DIRECTORY'
    AND per.uuid IN (SELECT uuid FROM
(SELECT count(*) AS antal, x.uuid AS uuid FROM
(SELECT p.person_uuid AS uuid, u.user_type, u.user_id FROM users u JOIN persons_users p ON u.id = p.user_id WHERE u.user_type = 'ACTIVE_DIRECTORY') x
GROUP BY x.uuid) y
WHERE y.antal > 1)
) yy
WHERE yy.prime = 1
  AND yy.user_id NOT IN (SELECT user_id FROM os2rollekatalog_kalundborg.users);

# find all users with more than one AD account
SELECT CONCAT(p.firstname, ' ', p.surname) as name,
       u.user_id,
       u.prime
FROM persons p
JOIN persons_users pu ON p.uuid = pu.person_uuid
JOIN users u ON pu.user_id = u.id
WHERE u.user_type = 'ACTIVE_DIRECTORY'
  AND p.uuid IN
    (SELECT person_uuid
       FROM persons_users
      WHERE user_id IN
         (SELECT id
            FROM users
           WHERE user_type = 'ACTIVE_DIRECTORY'
             AND prime = 0))
 ORDER BY p.firstname ASC;


# AD konti som ikke længere sendes til OS2rollekatalog fordi der ikke er noget tilhørsforhold

SELECT p.uuid, CONCAT(p.firstname, ' ', p.surname), u.user_id
  FROM persons p
  LEFT JOIN affiliations a ON a.person_uuid = p.uuid
  JOIN persons_users pu ON pu.person_uuid = p.uuid
  JOIN users u ON pu.user_id = u.id
  WHERE a.orgunit_uuid IS NULL
    AND u.user_type = 'ACTIVE_DIRECTORY';

# hvem har logget ind de sidste 6 måneder, og som vil miste rettigheder i fremtiden

SELECT uuid, user_id, name, active, email FROM os2rollekatalog_roskilde.users WHERE uuid IN (
SELECT DISTINCT entity_id FROM os2rollekatalog_roskilde.audit_log WHERE entity_id in (
SELECT uuid FROM os2rollekatalog_roskilde.users WHERE user_id in (
SELECT u.user_id
  FROM persons p
  LEFT JOIN affiliations a ON a.person_uuid = p.uuid
  JOIN persons_users pu ON pu.person_uuid = p.uuid
  JOIN users u ON pu.user_id = u.id
  WHERE a.orgunit_uuid IS NULL
    AND u.user_type = 'ACTIVE_DIRECTORY')));

