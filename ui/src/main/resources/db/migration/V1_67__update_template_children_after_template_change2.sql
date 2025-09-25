UPDATE email_template_children c
INNER JOIN email_templates t ON t.id = c.email_template_id
SET c.days_before_event = 5
WHERE t.template_type = 'SUBSTITUTE_STOPS';