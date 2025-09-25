package dk.digitalidentity.sofd.log;

import dk.digitalidentity.sofd.dao.AuditLogDao;
import dk.digitalidentity.sofd.dao.model.AuditLog;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AuditLogger {

	@Autowired
	private AuditLogDao auditLogDao;

	public void log(Loggable entity, EventType eventType, String message) {
		log(entity.getEntityId(), entity.getEntityType(), eventType, entity.getEntityName(), message);
	}

	public void log(String entityId, EntityType entityType, EventType eventType, String entityName, String message) {
		var userId = SecurityUtil.getUser() == null ? "system" : SecurityUtil.getUser();
		log( entityId, entityType, eventType, entityName, message, userId);
	}

	public void log(String entityId, EntityType entityType, EventType eventType, String entityName, String message, String userId) {
		AuditLog entry = new AuditLog();
		entry.setTimestamp(new Date());
		entry.setEntityId(entityId);
		entry.setEntityType(entityType);
		entry.setEventType(eventType);
		entry.setEntityName(entityName);
		entry.setMessage(message);
		entry.setUserId(userId);
		auditLogDao.save(entry);
	}
	
}