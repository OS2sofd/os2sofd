INSERT INTO it_systems (name, identifier, system_type, subscribed_to) select 'SOFD Core', 'sofdcore', 'SAML', master_id FROM it_systems_master WHERE name = 'SOFD Core';
