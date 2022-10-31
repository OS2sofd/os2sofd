ALTER TABLE fh_facet_value ADD affiliation_id BIGINT NULL;
ALTER TABLE fh_facet_value ADD CONSTRAINT fk_facet_value_affiliation_id FOREIGN KEY (affiliation_id) REFERENCES affiliations(id);