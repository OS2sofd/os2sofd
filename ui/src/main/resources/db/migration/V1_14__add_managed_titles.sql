CREATE TABLE managed_titles (
  id                 BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name               VARCHAR(255) NOT NULL,
  master             VARCHAR(64) NOT NULL,
  orgunit_uuid       VARCHAR(36) NOT NULL,
   
  CONSTRAINT managed_titles_ibfk1 FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);

CREATE TABLE managed_titles_aud (
  id                 BIGINT NOT NULL,
  rev                BIGINT NOT NULL,
  revtype            TINYINT DEFAULT NULL,
 
  name               VARCHAR(255) DEFAULT NULL,
  master             VARCHAR(64) DEFAULT NULL,
  orgunit_uuid       VARCHAR(36) NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT managed_titles_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);