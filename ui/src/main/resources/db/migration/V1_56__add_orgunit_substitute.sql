CREATE TABLE substitute_org_unit_assignment (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  substitute_context_id        BIGINT NOT NULL,
  org_unit_uuid                VARCHAR(36) NOT NULL,
  substitute_uuid              VARCHAR(36) NOT NULL,
  created                      datetime NOT NULL DEFAULT now(),
  changed                      datetime NOT NULL DEFAULT now(),

  CONSTRAINT substitute_org_unit_assignments_ibfk_1 FOREIGN KEY (substitute_context_id) REFERENCES substitute_context (id) ON DELETE CASCADE,
  CONSTRAINT substitute_org_unit_assignment_orgunit_ibfk_2 FOREIGN KEY (org_unit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE,
  CONSTRAINT substitute_org_unit_assignment_substitute_ibfk_3 FOREIGN KEY (substitute_uuid) REFERENCES persons (uuid) ON DELETE CASCADE
);

CREATE TABLE substitute_org_unit_assignment_aud (
  id                           BIGINT NOT NULL,
  rev                          BIGINT NOT NULL,
  revtype                      TINYINT DEFAULT NULL,

  substitute_context_id        BIGINT DEFAULT NULL,
  org_unit_uuid                VARCHAR(36) DEFAULT NULL,
  substitute_uuid              VARCHAR(36) DEFAULT NULL,
  created                      datetime DEFAULT NULL,
  changed                      datetime DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT substitute_org_unit_assignment_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

ALTER TABLE substitute_context ADD COLUMN assignable_to_org_unit BOOLEAN NOT NULL DEFAULT FALSE;