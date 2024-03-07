CREATE OR REPLACE view `view_odata_affiliation_function` AS
SELECT
       f.id
       , f.function
       , a.uuid as affiliation_uuid
FROM affiliations_function f
INNER JOIN affiliations a ON a.id = f.affiliation_id