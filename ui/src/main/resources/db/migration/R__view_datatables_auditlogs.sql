CREATE OR REPLACE VIEW view_datatables_auditlogs AS (
	SELECT a.id, a.timestamp, a.user_id, a.entity_type, COALESCE(a.entity_name, a.entity_id) AS entity_name, a.event_type, a.message
	FROM audit_log a
);