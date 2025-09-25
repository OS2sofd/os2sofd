-- cleanup orphan persons_leave records
DELETE FROM persons_leave WHERE
    id NOT IN (SELECT leave_id FROM persons WHERE leave_id IS NOT NULL);

-- cleanup orphan posts records
DELETE FROM posts WHERE
    id NOT IN (SELECT registered_post_address_id FROM persons WHERE registered_post_address_id IS NOT NULL)
    AND id NOT IN (SELECT residence_post_address_id FROM persons WHERE residence_post_address_id IS NOT NULL)
    AND id NOT IN (SELECT post_id FROM orgunits_posts WHERE post_id IS NOT NULL);
