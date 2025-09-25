CREATE OR REPLACE VIEW view_notifications AS SELECT
		id,
		notification_type,
		affected_entity_uuid,
		affected_entity_type,
		affected_entity_name,
		active,
		message,
		SUBSTRING(created, 1, 19) AS created,
		SUBSTRING(last_updated, 1, 19) AS last_updated,
		event_date,
		admin_uuid,
		admin_name
	FROM notifications;