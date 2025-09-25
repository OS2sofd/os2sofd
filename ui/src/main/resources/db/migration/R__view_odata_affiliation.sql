CREATE OR REPLACE view view_odata_affiliation
AS
  SELECT affiliations.id                        AS id,
         affiliations.uuid                      AS uuid,
         affiliations.master                    AS master,
         affiliations.master_id                 AS master_id,
         affiliations.start_date                AS start_date,
         affiliations.stop_date                 AS stop_date,
         affiliations.deleted                   AS deleted,
         COALESCE(workplace.orgunit_uuid, affiliations.alt_orgunit_uuid, affiliations.orgunit_uuid) AS orgunit_uuid,
         affiliations.orgunit_uuid              AS source_orgunit_uuid,
         affiliations.alt_orgunit_uuid          AS alternative_orgunit_uuid,
         affiliations.person_uuid               AS person_uuid,
         affiliations.employee_id               AS employee_id,
         affiliations.employment_terms          AS employment_terms,
         affiliations.employment_terms_text     AS employment_terms_text,
         affiliations.pay_grade                 AS pay_grade,
         affiliations.wage_step                 AS wage_step,
         affiliations.working_hours_denominator AS working_hours_denominator,
         affiliations.working_hours_numerator   AS working_hours_numerator,
         affiliations.affiliation_type          AS affiliation_type,
         affiliations.local_extensions          AS local_extensions,
         affiliations.position_id               AS position_id,
         CASE WHEN TRIM(IFNULL(affiliations.position_display_name,'')) <> '' THEN affiliations.position_display_name WHEN prof.id IS NOT NULL AND setting.disable_professions IS NULL THEN prof.name ELSE affiliations.position_name END AS position_name,
         affiliations.prime                     AS prime,
         affiliations.position_type_id          AS position_type_id,
         affiliations.position_type_name        AS position_type_name,
         affiliations.superior_level            AS superior_level,
         affiliations.subordinate_level         AS subordinate_level,
         orgunits.belongs_to                    AS organisation_id,
         affiliations.deactivate_and_delete_rule AS deactivate_and_delete_rule,
         affiliations.vendor					AS vendor,
         affiliations.internal_reference		AS internal_reference
  FROM   affiliations
  LEFT JOIN (
        SELECT aw.orgunit_uuid, aw.affiliation_id
        FROM affiliations_workplaces aw
        WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate()
  ) workplace ON workplace.affiliation_id = affiliations.id
  LEFT JOIN professions prof on prof.id = affiliations.profession_id
  LEFT JOIN (
    SELECT 1 AS disable_professions FROM settings WHERE setting_key = 'DISABLE_PROFESSIONS' AND setting_value = 'true'
  ) setting ON 1 = 1
  INNER JOIN orgunits ON orgunits.uuid = affiliations.orgunit_uuid;