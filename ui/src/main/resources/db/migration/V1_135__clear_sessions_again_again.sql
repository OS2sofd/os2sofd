# Clear sessions due to Spring Security/Spring Session upgrade
CREATE TABLE IF NOT EXISTS SPRING_SESSION (primary_id CHAR(36) NOT NULL);
DELETE FROM SPRING_SESSION;