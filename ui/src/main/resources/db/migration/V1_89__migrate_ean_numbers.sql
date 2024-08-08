CREATE TABLE ean (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  number                       BIGINT NOT NULL,
  master                       VARCHAR(64) NOT NULL,
  prime                        BOOLEAN NOT NULL DEFAULT false,
  orgunit_uuid                 VARCHAR(36) NOT NULL,

  CONSTRAINT ean_orgunit_ibfk_1 FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);

CREATE TABLE ean_aud (
  id                           BIGINT NOT NULL,
  rev                          BIGINT NOT NULL,
  revtype                      TINYINT DEFAULT NULL,

  number                       BIGINT DEFAULT NULL,
  master                       VARCHAR(64) DEFAULT NULL,
  prime                        BOOLEAN DEFAULT NULL,
  orgunit_uuid                 VARCHAR(36) DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT ean_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

INSERT INTO ean(number, master, prime, orgunit_uuid) SELECT ean, master, true, uuid FROM orgunits WHERE ean IS NOT NULL;

ALTER TABLE orgunits DROP COLUMN ean;
ALTER TABLE orgunits_aud DROP COLUMN ean;