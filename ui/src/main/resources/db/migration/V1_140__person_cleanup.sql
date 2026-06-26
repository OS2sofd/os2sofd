-- do not support NEVER, anyone with that setting will revert to default (36 months currently)
delete from settings where setting_key = 'PersonDeletePeriod' and setting_value = 'NEVER';
