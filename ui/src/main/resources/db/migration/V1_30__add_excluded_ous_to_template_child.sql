CREATE TABLE email_template_child_org_unit (
	id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	email_template_child_id      BIGINT NOT NULL,
  	org_unit_uuid                VARCHAR(36) NOT NULL,
  	
  	CONSTRAINT fk_email_template_child_org_unit_template_child FOREIGN KEY (email_template_child_id) REFERENCES email_template_children(id) ON DELETE CASCADE,
  	CONSTRAINT fk_email_template_child_org_unit_org_unit FOREIGN KEY (org_unit_uuid) REFERENCES orgunits(uuid) ON DELETE CASCADE
);