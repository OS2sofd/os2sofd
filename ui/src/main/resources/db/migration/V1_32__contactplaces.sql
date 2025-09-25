CREATE TABLE contactplaces (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  orgunit_uuid                   VARCHAR(36) NOT NULL,
  synchronized_to_organisation   BOOLEAN NOT NULL DEFAULT 0,
  deleted                        BOOLEAN NOT NULL DEFAULT 0,

  CONSTRAINT contactplace_orgunit_uuid FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);

CREATE TABLE contactplaces_kle (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  contactplace_id              BIGINT NOT NULL,
  kle_id                       BIGINT NOT NULL,

  CONSTRAINT contactplaces_kle_contactplace_id FOREIGN KEY (contactplace_id) REFERENCES contactplaces(id) ON DELETE CASCADE,
  CONSTRAINT contactplaces_kle_kle_id FOREIGN KEY (kle_id) REFERENCES kle(id) ON DELETE CASCADE
);

CREATE TABLE contactplaces_orgunits (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  contactplace_id              BIGINT NOT NULL,
  orgunit_uuid                 VARCHAR(36) NOT NULL,
  deleted                      BOOLEAN NOT NULL DEFAULT 0,

  CONSTRAINT contactplaces_orgunits_contactplace_id FOREIGN KEY (contactplace_id) REFERENCES contactplaces(id) ON DELETE CASCADE,
  CONSTRAINT contactplaces_orgunits_orgunit_uuid FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);
