UPDATE email_template_children c
INNER JOIN email_templates t ON t.id = c.email_template_id
SET c.only_manual_recipients = 1
WHERE t.template_type = 'NEW_MANAGER';