-- make sure new manager table exists
DROP TABLE IF EXISTS orgunits_manager;
CREATE TABLE orgunits_manager
(
    orgunit_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    manager_uuid VARCHAR(36) NOT NULL,
    inherited    TINYINT(1)  NOT NULL,
    name         VARCHAR(512) NOT NULL,
    source       VARCHAR(50)  NOT NULL
);

-- procedure to update managers
DROP PROCEDURE IF EXISTS update_orgunit_manager_recursive;
DELIMITER $$
CREATE PROCEDURE update_orgunit_manager_recursive(IN in_orgunit_uuid VARCHAR(36), IN in_initialize BOOLEAN)
BEGIN
    INSERT INTO orgunits_manager (orgunit_uuid, manager_uuid, inherited, name, source)
    WITH RECURSIVE cte AS
                       (
                           SELECT
                               o.uuid,
                               o.parent_uuid,
                               IFNULL(IFNULL(o.selected_manager_uuid,o.imported_manager_uuid), pm.manager_uuid) AS manager_uuid,
                               ISNULL(IFNULL(o.selected_manager_uuid,o.imported_manager_uuid)) AS inherited,
                               CASE
                                   WHEN o.selected_manager_uuid IS NOT NULL THEN 'SELECTED'
                                   WHEN o.imported_manager_uuid IS NOT NULL THEN 'IMPORTED'
                                   ELSE pm.source
                                   END AS source
                           FROM orgunits o
                                    LEFT JOIN orgunits_manager pm ON pm.orgunit_uuid = o.parent_uuid
                           WHERE
                               o.deleted = 0
                             and
                               (
                                   o.uuid = in_orgunit_uuid -- when requesting subtree
                                       or (o.parent_uuid is null and in_initialize) -- when requesting table initialize
                                   )


                           UNION ALL

                           SELECT
                               o.uuid,
                               o.parent_uuid,
                               IFNULL(IFNULL(o.selected_manager_uuid,o.imported_manager_uuid), cte.manager_uuid) AS manager_uuid,
                               ISNULL(IFNULL(o.selected_manager_uuid,o.imported_manager_uuid)) AS inherited,
                               CASE
                                   WHEN o.selected_manager_uuid IS NOT NULL THEN 'SELECTED'
                                   WHEN o.imported_manager_uuid IS NOT NULL THEN 'IMPORTED'
                                   ELSE cte.source
                                   END AS source
                           FROM orgunits o
                                    INNER JOIN cte ON cte.uuid = o.parent_uuid
                           WHERE o.deleted = 0
                       )
    SELECT
        cte.uuid AS orgunit_uuid,
        cte.manager_uuid,
        cte.inherited,
        IFNULL(p.chosen_name, CONCAT(p.firstname,' ',p.surname)) AS name,
        cte.source
    FROM cte
             INNER JOIN persons p ON p.uuid = cte.manager_uuid
    ON DUPLICATE KEY UPDATE
                         manager_uuid = VALUES(manager_uuid),
                         inherited    = VALUES(inherited),
                         name         = VALUES(name),
                         source       = VALUES(source);
END$$
DELIMITER ;

-- load initial data
CALL update_orgunit_manager_recursive(null,true);

-- clean up old triggers if they exist
DROP TRIGGER IF EXISTS orgunits_manager_insert;
DROP TRIGGER IF EXISTS orgunits_manager_update;
DROP TRIGGER IF EXISTS orgunits_manager_delete;

DELIMITER $$
-- insert trigger
CREATE TRIGGER orgunits_manager_insert
AFTER INSERT ON orgunits
FOR EACH ROW
BEGIN
    CALL update_orgunit_manager_recursive(NEW.uuid, false);
END$$

-- update trigger
CREATE TRIGGER orgunits_manager_update
AFTER UPDATE ON orgunits
FOR EACH ROW
BEGIN
    CALL update_orgunit_manager_recursive(NEW.uuid, false);
END$$

-- delete trigger
CREATE TRIGGER orgunits_manager_delete
AFTER DELETE ON orgunits
FOR EACH ROW
BEGIN
    DELETE FROM orgunits_manager
    WHERE orgunit_uuid = OLD.uuid;
END$$

DELIMITER ;
