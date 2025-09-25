CREATE TABLE chart (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  uuid                         VARCHAR(36) NOT NULL,
  name                         VARCHAR(255) NOT NULL,
  inherit_enabled              BOOLEAN NOT NULL DEFAULT 0,
  leader_enabled               BOOLEAN NOT NULL DEFAULT 0,
  depth_limit                  VARCHAR(255) NOT NULL,
  vertical_start               VARCHAR(255) NOT NULL,
  style                        TEXT NOT NULL
);

CREATE TABLE chart_orgunit (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  chart_id                     BIGINT NOT NULL,
  orgunit_uuid                 VARCHAR(36) NOT NULL,

  CONSTRAINT chart_orgunit_chart_id FOREIGN KEY (chart_id) REFERENCES chart(id) ON DELETE CASCADE,
  CONSTRAINT chart_orgunit_orgunit_uuid FOREIGN KEY (orgunit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);