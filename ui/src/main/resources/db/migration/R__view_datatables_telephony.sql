-- one-shots, to cleanup old views
DROP VIEW IF EXISTS view_datatables_telephony_subview;

CREATE OR REPLACE VIEW subview_datatables_telephony AS
SELECT
    tp.id,
    tp.master,
    tp.phone_number,
    tp.vendor,
    tp.account_number,
    tp.ean,
    tp.phone_type,
    tp.visibility,
    tp.function_type_id,
    tp.person_uuid,
    tp.person_name,
    tpo.orgunit_uuid,
    tpo.orgunit_name
FROM telephony_phones tp
LEFT JOIN telephony_phones_orgunits tpo ON tpo.telephony_phones_id = tp.id;

CREATE OR REPLACE VIEW view_datatables_telephony AS
SELECT
  id,
  master,
  phone_number,
  vendor,
  account_number,
  ean,
  phone_type,
  visibility,
  function_type_id,
  person_uuid,
  person_name,
  GROUP_CONCAT(orgunit_uuid SEPARATOR ',') AS orgunit_uuid,
  GROUP_CONCAT(orgunit_name SEPARATOR ',') AS orgunit_name,
  count(person_uuid) > 0 or count(orgunit_uuid) > 0 as assigned
FROM subview_datatables_telephony
GROUP BY id;