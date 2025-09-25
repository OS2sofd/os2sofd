CREATE OR REPLACE view `view_odata_function_assignment`
AS
  SELECT
    fa.id
    ,fa.start_date
    ,fa.stop_date
    ,fa.function_id
    ,fa.affiliation_id
  FROM fh_function_assignment fa;