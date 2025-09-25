-- it also gets created in V1_64, but here we can tune it if needed
CREATE OR REPLACE VIEW view_adm_organisation AS SELECT id FROM organisations WHERE short_name = 'ADMORG';
