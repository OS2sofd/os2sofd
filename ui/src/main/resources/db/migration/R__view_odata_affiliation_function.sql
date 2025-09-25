CREATE OR REPLACE view `view_odata_affiliation_function` AS
SELECT
       f.id
       ,f.function
       ,f.affiliation_id
FROM affiliations_function f;