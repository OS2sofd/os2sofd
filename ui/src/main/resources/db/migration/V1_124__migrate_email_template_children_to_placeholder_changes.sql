UPDATE email_template_children 
SET message = REPLACE(REPLACE(message, '{kontonavn}', '{ad-brugernavn}'), '{exchange}', '{exchange-email}')
WHERE email_template_id NOT IN (
    SELECT id 
    FROM email_templates
    WHERE template_type IN ('EXCHANGE_CREATE_EMPLOYEE', 'EXCHANGE_CREATE_EMPLOYEE2', 'EXCHANGE_CREATE_MANAGER')
);

UPDATE email_template_children 
SET message = REPLACE(REPLACE(message, '{kontonavn}', '{exchange-email}'), '{exchange}', '{exchange-email}')
WHERE email_template_id IN (
    SELECT id 
    FROM email_templates
    WHERE template_type IN ('EXCHANGE_CREATE_EMPLOYEE', 'EXCHANGE_CREATE_EMPLOYEE2', 'EXCHANGE_CREATE_MANAGER')
);