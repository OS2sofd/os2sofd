package dk.digitalidentity.sofd.log;

import dk.digitalidentity.sofd.dao.AuditLogDao;
import dk.digitalidentity.sofd.dao.model.AuditLog;
import dk.digitalidentity.sofd.dao.model.Client;
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
		log(entityId, entityType, eventType, entityName, message, null);
	}

	// fallbackUserId is only used when neither a logged in user nor an OnBehalfOf header identifies the actor.
	// It exists for API clients that still send the actor in the request body instead of the header.
	// TODO: remove this variant (and the userId body fields in SubstituteApiController) once all manager UI
	// installations send the OnBehalfOf header (GH-53)
	public void log(String entityId, EntityType entityType, EventType eventType, String entityName, String message, String fallbackUserId) {
		String userId = resolveUserId();
		if (userId == null) {
			userId = (fallbackUserId != null && !fallbackUserId.isBlank()) ? fallbackUserId : "system";
		}

		AuditLog entry = new AuditLog();
		entry.setTimestamp(new Date());
		entry.setEntityId(entityId);
		entry.setEntityType(entityType);
		entry.setEventType(eventType);
		entry.setEntityName(entityName);
		entry.setMessage(message);
		entry.setUserId(userId);
		entry.setClientName(resolveClientName());
		auditLogDao.save(entry);
	}

	// a logged in user is the actor. For API calls the actor is the person the integration acts on behalf of
	// (OnBehalfOf header) when supplied. Null when neither identifies an actor
	private static String resolveUserId() {
		String user = SecurityUtil.getUser();
		if (user != null) {
			return user;
		}

		return SecurityUtil.getOnBehalfOf();
	}

	// the API client the call came in on, so the trail shows which integration performed the change
	private static String resolveClientName() {
		Client client = SecurityUtil.getClient();

		return (client != null) ? client.getName() : null;
	}
}
