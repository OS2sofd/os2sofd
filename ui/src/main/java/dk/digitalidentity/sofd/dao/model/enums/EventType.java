package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum EventType {
	SAVE("html.enum.eventType.save"), 
	DELETE("html.enum.eventType.delete"), 
	LOGIN("html.enum.eventType.login"), 
	FAILED_LOGIN("html.enum.eventType.failedLogin"), 
	VIEW_PERSON("html.enum.eventType.view_person"), 
	DONWLOAD_REPORT("html.enum.eventType.downloadReport"), 
	VIEW_REPORT("html.enum.eventType.viewReport"), 
	MESSAGE_SEND("html.enum.eventType.messageSend"), 
	PERSON_CREATE("html.enum.eventType.personCreate"), 
	USER_DELETED("html.enum.eventType.userDeleted"), 
	AFFILIATION_CREATED("html.enum.eventType.affiliationCreated"),
	PERSON_CHANGED("html.enum.eventType.personChanged"),
	RERUN("html.enum.eventType.rerun");

	private String message;
	
	private EventType(String message) {
		this.message = message;
	}
}
