package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.dto;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.AuditLogView;
import dk.digitalidentity.sofd.dao.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
	private long id;
	private Date timestamp;
	private String entityName;
	private String entityType;
	private String eventType;
	private String userId;
	private String clientName;
	private String message;

	public AuditLogDTO(AuditLogView auditlog, MessageSource messageSource, Locale locale) {
		this.id = auditlog.getId();
		this.timestamp = auditlog.getTimestamp();
		this.entityName = auditlog.getEntityName();
		this.entityType = messageSource.getMessage(auditlog.getEntityType().getMessage(), null, locale);
		this.eventType = messageSource.getMessage(auditlog.getEventType().getMessage(), null, locale);
		this.userId = auditlog.getUserId();
		this.clientName = clientNameOrCore(auditlog.getClientName());
		this.message = StringUtils.abbreviate(auditlog.getMessage(), 200);
	}
	
	public AuditLogDTO(AuditLog auditlog, MessageSource messageSource, Locale locale) {
		this.id = auditlog.getId();
		this.timestamp = auditlog.getTimestamp();
		this.entityName = auditlog.getEntityName();
		this.entityType = messageSource.getMessage(auditlog.getEntityType().getMessage(), null, locale);
		this.eventType = messageSource.getMessage(auditlog.getEventType().getMessage(), null, locale);
		this.userId = auditlog.getUserId();
		this.clientName = clientNameOrCore(auditlog.getClientName());
		this.message = auditlog.getMessage();
	}

	// entries without an API client were made in OS2sofd itself (a logged in user or a background task)
	public static String clientNameOrCore(String clientName) {
		return StringUtils.isNotBlank(clientName) ? clientName : "OS2sofd";
	}
}
