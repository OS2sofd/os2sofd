CREATE TABLE student (
   id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   uuid                    VARCHAR(36) NOT NULL,
   user_id                 VARCHAR(255) NOT NULL UNIQUE,
   name                    VARCHAR(255) NOT NULL,
   cpr                     VARCHAR(10) NULL,
   disabled                BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE student ADD INDEX idx_student_uuid (uuid);
ALTER TABLE student ADD INDEX idx_student_cpr (cpr);

CREATE TABLE student_institution_numbers (
   id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   student_id              BIGINT NOT NULL,
   institution_number      VARCHAR(255) NULL,

   CONSTRAINT fk_student_institution_numbers_on_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

CREATE TABLE institution (
   id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   uuid                    VARCHAR(36) NOT NULL,
   institution_number      VARCHAR(255) NOT NULL,
   name                    VARCHAR(255) NOT NULL
);

CREATE TABLE student_aud (
  id                       BIGINT NOT NULL,
  rev                      BIGINT NOT NULL,
  revtype                  TINYINT DEFAULT NULL,

  uuid                     VARCHAR(36) DEFAULT NULL,
  user_id                  VARCHAR(255) DEFAULT NULL,
  name                     VARCHAR(255) DEFAULT NULL,
  cpr                      VARCHAR(10) DEFAULT NULL,
  disabled                 BOOLEAN DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT student_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

CREATE TABLE institution_aud (
  id                       BIGINT NOT NULL,
  rev                      BIGINT NOT NULL,
  revtype                  TINYINT DEFAULT NULL,

  uuid                     VARCHAR(36) DEFAULT NULL,
  institution_number       VARCHAR(255) DEFAULT NULL,
  name                     VARCHAR(255) DEFAULT NULL,

  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT institution_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);
