CREATE OR REPLACE view `view_odata_function_assignment`
AS
  SELECT
    fa.id
    ,fa.start_date
    ,fa.stop_date
    ,fa.function_id
    ,a.uuid as affiliation_uuid
  FROM fh_function_assignment fa
  INNER JOIN affiliations a on a.id = fa.affiliation_id;
