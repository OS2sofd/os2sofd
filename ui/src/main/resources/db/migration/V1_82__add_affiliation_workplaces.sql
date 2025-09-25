CREATE TABLE affiliations_workplaces (
   id                   BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
   start_date           date NOT NULL,
   stop_date            date NOT NULL,
   affiliation_id       BIGINT NOT NULL,
   orgunit_uuid         VARCHAR(36) NOT NULL,
   CONSTRAINT fk_affiliations_workplaces__affiliations FOREIGN KEY (affiliation_id) REFERENCES affiliations(id) ON DELETE CASCADE,
   CONSTRAINT fk_affiliations_workplaces__orgunits FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);

CREATE TABLE affiliations_workplaces_aud (
   id                   BIGINT NOT NULL,
   rev                  BIGINT NOT NULL,
   revtype              TINYINT DEFAULT NULL,

   start_date           date DEFAULT NULL,
   stop_date            date DEFAULT NULL,
   affiliation_id       BIGINT DEFAULT NULL,
   orgunit_uuid         VARCHAR(36) DEFAULT NULL,

   PRIMARY KEY (id,rev),
   KEY rev (rev),
   CONSTRAINT affiliations_workplaces_aud_aud_ibfk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);