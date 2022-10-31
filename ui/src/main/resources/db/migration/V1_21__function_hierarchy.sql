CREATE TABLE fh_function (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                         VARCHAR(255) NOT NULL,
  description                  TEXT NULL
);

CREATE TABLE fh_facet (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                         VARCHAR(255) NOT NULL,
  type                         VARCHAR(255) NOT NULL,
  pattern                      VARCHAR(255) NULL,
  description                  TEXT NULL
);

CREATE TABLE fh_list_item (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  text                         VARCHAR(255) NOT NULL,
  facet_id                     BIGINT NOT NULL,
  
  CONSTRAINT fk_list_item_facet FOREIGN KEY (facet_id) REFERENCES fh_facet(id) ON DELETE CASCADE
);

CREATE TABLE fh_function_facet (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  function_id                  BIGINT NOT NULL,
  facet_id                     BIGINT NOT NULL,
  
  CONSTRAINT fk_function_facet_facet FOREIGN KEY (facet_id) REFERENCES fh_facet(id) ON DELETE CASCADE,
  CONSTRAINT fk_function_facet_function FOREIGN KEY (function_id) REFERENCES fh_function(id) ON DELETE CASCADE
);

CREATE TABLE fh_function_assignment (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  start_date                   DATETIME NOT NULL,
  stop_date                    DATETIME NOT NULL,
  function_id                  BIGINT NOT NULL,
  affiliation_id               BIGINT NOT NULL,
  
  CONSTRAINT fk_function_assignment_function FOREIGN KEY (function_id) REFERENCES fh_function(id) ON DELETE CASCADE,
  CONSTRAINT fk_function_assignment_affiliation FOREIGN KEY (affiliation_id) REFERENCES affiliations(id) ON DELETE CASCADE
);

CREATE TABLE fh_facet_value (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  text                         VARCHAR(255) NULL,
  function_assignment_id       BIGINT NOT NULL,
  facet_id                     BIGINT NOT NULL,
  list_item_id                 BIGINT NULL,
  
  CONSTRAINT fk_facet_value_function_assignment FOREIGN KEY (function_assignment_id) REFERENCES fh_function_assignment(id) ON DELETE CASCADE,
  CONSTRAINT fk_facet_value_facet FOREIGN KEY (facet_id) REFERENCES fh_facet(id) ON DELETE CASCADE,
  CONSTRAINT fk_facet_value_list_item FOREIGN KEY (list_item_id) REFERENCES fh_list_item(id) ON DELETE CASCADE
);

CREATE TABLE fh_facet_value_orgunit (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  facet_value_id               BIGINT NOT NULL,
  orgunit_uuid                 VARCHAR(36) NOT NULL,
  
  CONSTRAINT fk_facet_value_orgunit_facet_value FOREIGN KEY (facet_value_id) REFERENCES fh_facet_value(id) ON DELETE CASCADE,
  CONSTRAINT fk_facet_value_orgunit_orgunit FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);