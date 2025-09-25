ALTER TABLE email_template_children ADD COLUMN days_before_event BIGINT NOT NULL DEFAULT 0;

INSERT INTO email_templates (template_type) VALUES ('RESIGNATION');
INSERT INTO email_templates (template_type) VALUES ('AFFILIATION_EXPIRE_REMINDER');

UPDATE email_template_children c
  INNER JOIN email_templates et ON et.id = c.email_template_id
  SET c.email_template_id = (SELECT id FROM email_templates tt WHERE tt.template_type = 'RESIGNATION'), days_before_event = 30
  WHERE et.template_type = 'RESIGNATION_30';

UPDATE email_template_children c
  INNER JOIN email_templates et ON et.id = c.email_template_id
  SET c.email_template_id = (SELECT id FROM email_templates tt WHERE tt.template_type = 'RESIGNATION'), days_before_event = 5
  WHERE et.template_type = 'RESIGNATION_5';

UPDATE email_template_children c
  INNER JOIN email_templates et ON et.id = c.email_template_id
  SET c.email_template_id = (SELECT id FROM email_templates tt WHERE tt.template_type = 'AFFILIATION_EXPIRE_REMINDER'), days_before_event = 30
  WHERE et.template_type = 'AFFILIATION_EXPIRE_REMINDER_30';

UPDATE email_template_children c
  INNER JOIN email_templates et ON et.id = c.email_template_id
  SET c.email_template_id = (SELECT id FROM email_templates tt WHERE tt.template_type = 'AFFILIATION_EXPIRE_REMINDER'), days_before_event = 90
  WHERE et.template_type = 'AFFILIATION_EXPIRE_REMINDER_90';

DELETE FROM email_templates 
WHERE template_type IN ('RESIGNATION_30', 'RESIGNATION_5', 'AFFILIATION_EXPIRE_REMINDER_30', 'AFFILIATION_EXPIRE_REMINDER_90');