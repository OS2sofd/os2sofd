-- create trigger that handles reverse cascade deletions for persons
DROP TRIGGER IF EXISTS reverse_cascade_persons;
DELIMITER $$
CREATE TRIGGER reverse_cascade_persons
    AFTER DELETE
    ON persons FOR EACH ROW
    BEGIN
        DELETE FROM persons_leave WHERE id = old.leave_id;
        DELETE FROM posts WHERE id = old.registered_post_address_id;
        DELETE FROM posts WHERE id = old.residence_post_address_id;
    END$$
DELIMITER ;