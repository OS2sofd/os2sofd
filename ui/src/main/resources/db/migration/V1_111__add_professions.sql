CREATE TABLE professions (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name                    VARCHAR(255) NOT NULL,
    organisation_id         BIGINT NOT NULL,

    CONSTRAINT `fk_professions__organisations` FOREIGN KEY (organisation_id) REFERENCES organisations(id) ON DELETE CASCADE,
    CONSTRAINT `uc_professions__name` UNIQUE (organisation_id,name)
);

CREATE TABLE profession_mappings (
    id                    BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    match_value           VARCHAR(255) NOT NULL,
    profession_id         BIGINT NOT NULL,

    CONSTRAINT `fk_profession_mappings__professions` FOREIGN KEY (profession_id) REFERENCES professions(id) ON DELETE CASCADE
);

ALTER TABLE affiliations ADD COLUMN profession_id BIGINT DEFAULT NULL;
ALTER TABLE affiliations ADD CONSTRAINT `fk_affiliations__professions` FOREIGN KEY (profession_id) REFERENCES professions(id) ON DELETE SET NULL;
