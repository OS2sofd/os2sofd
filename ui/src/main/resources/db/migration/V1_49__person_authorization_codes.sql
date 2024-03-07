ALTER TABLE persons DROP COLUMN authorization_code;
ALTER TABLE persons_aud DROP COLUMN authorization_code;

CREATE TABLE authorization_code (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  code                         VARCHAR(255) NOT NULL,
  name                         VARCHAR(255) NOT NULL,
  prime                        BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE authorization_code_aud (
  id                           BIGINT NOT NULL,
  rev                          BIGINT NOT NULL,
  revtype                      TINYINT DEFAULT NULL,
  
  code                         VARCHAR(255) DEFAULT NULL,
  name                         VARCHAR(255) DEFAULT NULL,
  prime                        BOOLEAN DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT authorization_code_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

CREATE TABLE persons_authorization_codes (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  person_uuid                  VARCHAR(36) NOT NULL,
  authorization_code_id        BIGINT NOT NULL,
  
  CONSTRAINT persons_authorization_codes_authorization_codes FOREIGN KEY (authorization_code_id) REFERENCES authorization_code (id) ON DELETE CASCADE,
  CONSTRAINT persons_authorization_codes_persons FOREIGN KEY (person_uuid) REFERENCES persons (uuid) ON DELETE CASCADE
);

CREATE TABLE persons_authorization_codes_aud (
  id                           BIGINT NOT NULL,
  rev                          BIGINT NOT NULL,
  revtype                      TINYINT DEFAULT NULL,

  person_uuid                  VARCHAR(255) DEFAULT NULL,
  authorization_code_id        VARCHAR(255) DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT persons_authorization_codes_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);
