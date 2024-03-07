ALTER TABLE email_template_children ADD COLUMN send_to VARCHAR(64) NULL;

UPDATE email_template_children SET send_to = 'SEND_TO_MANAGER_OR_SUBSTITUTES' WHERE send_to_substitute = 1;
UPDATE email_template_children SET send_to = 'SEND_TO_MANAGER' WHERE send_to_substitute = 0;

ALTER TABLE email_template_children DROP COLUMN send_to_substitute;