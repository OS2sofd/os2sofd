CREATE OR REPLACE VIEW view_datatables_accountorders AS
  SELECT ao.id                                     AS id,
         SUBSTRING(ao.activation_timestamp, 1, 16) AS activation_timestamp,
         ao.end_date                               AS end_date,
         ao.person_uuid                            AS person_uuid,
         CASE
           WHEN TRIM(IFNULL(p.chosen_name, '')) <> ''
             THEN p.chosen_name
           ELSE CONCAT(p.firstname, ' ', p.surname)
         END                                       AS person_name,
         CASE
           WHEN aff.id IS NULL THEN NULL
           ELSE CONCAT(
                  CASE
                    WHEN TRIM(IFNULL(aff.position_display_name,'')) <> ''
                      THEN aff.position_display_name
                    WHEN prof.id IS NOT NULL
                         AND NOT EXISTS (SELECT 1
                                         FROM   settings
                                         WHERE  setting_key   = 'DISABLE_PROFESSIONS'
                                           AND  setting_value = 'true')
                      THEN prof.name
                    ELSE aff.position_name
                  END,
                  ' i ',
                  CASE
                    WHEN TRIM(IFNULL(ou.display_name, '')) <> ''
                      THEN ou.display_name
                    ELSE ou.source_name
                  END)
         END                                       AS trigger_affiliation,
         CASE
           WHEN TRIM(IFNULL(ao.actual_user_id, '')) <> ''
             THEN ao.actual_user_id
           ELSE ao.requested_user_id
         END                                       AS user_id,
         ao.order_type                             AS order_type,
         ao.user_type                              AS user_type,
         ao.status                                 AS status
  FROM   account_orders ao
  -- INNER JOIN, as orders without a person were skipped by the old in-memory version of this report
  INNER JOIN persons p ON p.uuid = ao.person_uuid
  LEFT JOIN affiliations aff ON aff.id = ao.trigger_affiliation_id
  LEFT JOIN professions prof ON prof.id = aff.profession_id
  -- a scalar subquery rather than a join, so an affiliation with two overlapping workplaces
  -- cannot multiply the rows of this report (Affiliation.getCalculatedOrgUnit() picks the first one too)
  LEFT JOIN orgunits ou ON ou.uuid = COALESCE(
        (SELECT aw.orgunit_uuid
         FROM   affiliations_workplaces aw
         WHERE  aw.affiliation_id = aff.id
           AND  aw.start_date <= CURDATE() AND aw.stop_date >= CURDATE()
         LIMIT  1),
        aff.alt_orgunit_uuid,
        aff.orgunit_uuid);
