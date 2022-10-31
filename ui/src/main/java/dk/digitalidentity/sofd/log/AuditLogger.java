package dk.digitalidentity.sofd.log;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.AuditLogDao;
import dk.digitalidentity.sofd.dao.model.AuditLog;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.security.SecurityUtil;

@Component
public class AuditLogger {

	@Autowired
	private AuditLogDao auditLogDao;

	public void log(Loggable entity, EventType eventType, String message) {
		log(entity.getEntityId(), entity.getEntityType(), eventType, entity.getEntityName(), message);
	}
	
	public void log(String entityId, EntityType entityType, EventType eventType, String entityName, String message) {
		AuditLog entry = new AuditLog();
		entry.setTimestamp(new Date());
		entry.setEntityId(entityId);
		entry.setEntityType(entityType);
		entry.setEventType(eventType);
		entry.setEntityName(entityName);
		entry.setMessage(message);
		
		if (SecurityUtil.getUser() == null) {
			entry.setUserId("system");
		}
		else {
			entry.setUserId(SecurityUtil.getUser());
		}

		auditLogDao.save(entry);		
	}
}