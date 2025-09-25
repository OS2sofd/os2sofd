ALTER TABLE chart ADD COLUMN organisation_id BIGINT NULL;
UPDATE chart c INNER JOIN view_adm_organisation admo SET c.organisation_id = admo.id;
ALTER TABLE chart MODIFY COLUMN organisation_id BIGINT NOT NULL;

