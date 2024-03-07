ALTER TABLE active_directory_details ADD COLUMN kombit_uuid VARCHAR(36) NOT NULL DEFAULT '';
ALTER TABLE active_directory_details_aud ADD COLUMN kombit_uuid VARCHAR(36) NULL;

CREATE TABLE fk_org_uuids (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  person_uuid                  VARCHAR(36) NOT NULL,
  user_uuid                    VARCHAR(36) NOT NULL,
  kombit_uuid                  VARCHAR(36) NOT NULL
);