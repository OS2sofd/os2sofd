update email_template_children c
inner join email_templates t on t.id = c.email_template_id
set c.message = concat('<h2>{overskrift}</h2>',c.message)
where
    t.template_type in ('AD_CREATE_EMPLOYEE','AD_CREATE_SUBSTITUTE','EXCHANGE_CREATE_EMPLOYEE_EBOKS','NEW_EMPLOYEE_DIGITAL_POST')
  and c.raw_template = 0;