DELETE sa FROM substitute_assignment sa
INNER JOIN persons p ON p.uuid = sa.person_uuid
WHERE p.deleted = 1;