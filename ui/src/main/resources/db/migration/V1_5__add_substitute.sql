CREATE TABLE substitute_context (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                         VARCHAR(255) NOT NULL,
  identifier                   VARCHAR(255) NOT NULL,
  supports_constraints         TINYINT NOT NULL DEFAULT FALSE,
  can_be_deleted               TINYINT NOT NULL DEFAULT TRUE
);

INSERT INTO substitute_context (name, identifier, supports_constraints, can_be_deleted) VALUES ("SOFD Core", "SOFD", 1, 0);
INSERT INTO substitute_context (name, identifier, supports_constraints, can_be_deleted) VALUES ("Global", "GLOBAL", 0, 0);

CREATE TABLE substitute_assignment (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  substitute_context_id        BIGINT NOT NULL,
  person_uuid                  VARCHAR(36) NOT NULL,
  substitute_uuid              VARCHAR(36) NOT NULL,
  
  CONSTRAINT substitute_assignments_ibfk_1 FOREIGN KEY (substitute_context_id) REFERENCES substitute_context (id) ON DELETE CASCADE,
  CONSTRAINT substitute_assignments_ibfk_2 FOREIGN KEY (person_uuid) REFERENCES persons (uuid) ON DELETE CASCADE,
  CONSTRAINT substitute_assignments_ibfk_3 FOREIGN KEY (substitute_uuid) REFERENCES persons (uuid) ON DELETE CASCADE
);

CREATE TABLE substitute_assignment_aud (
  id                           BIGINT NOT NULL,
  rev                          BIGINT NOT NULL,
  revtype                      TINYINT DEFAULT NULL,
 
  substitute_context_id        BIGINT DEFAULT NULL,
  person_uuid                  VARCHAR(36) DEFAULT NULL,
  substitute_uuid              VARCHAR(36) DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT substitute_assignment_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

CREATE TABLE substitute_assignment_orgunit (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  substitute_assignment_id     BIGINT NOT NULL,
  orgunit_uuid                 VARCHAR(36) NOT NULL,

  CONSTRAINT substitute_assignment_orgunit_ibfk_1 FOREIGN KEY (orgunit_uuid) REFERENCES orgunits (uuid) ON DELETE CASCADE,
  CONSTRAINT substitute_assignment_orgunit_ibfk_2 FOREIGN KEY (substitute_assignment_id) REFERENCES substitute_assignment (id) ON DELETE CASCADE
);

CREATE TABLE substitute_assignment_orgunit_aud (
  id                           BIGINT NOT NULL,
  rev                          BIGINT NOT NULL,
  revtype                      TINYINT DEFAULT NULL,
 
  substitute_assignment_id     BIGINT DEFAULT NULL,
  orgunit_uuid                 VARCHAR(36) DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT substitute_assignment_orgunit_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

ALTER TABLE email_template_children ADD COLUMN send_to_substitute TINYINT NOT NULL DEFAULT FALSE;
