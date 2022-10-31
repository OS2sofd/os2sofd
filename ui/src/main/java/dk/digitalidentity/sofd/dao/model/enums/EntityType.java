package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum EntityType {
	ORGANISATION("html.enum.entityType.organisation"), 
	ORGUNIT("html.enum.entityType.orgunit"), 
	PERSON("html.enum.entityType.person"), 
	CLIENT("html.enum.entityType.client"), 
	REPORT("html.enum.entityType.report"),
	EMAIL("html.enum.entityType.email"),
	CPR("html.enum.entityType.cpr");
	
	private String message;
	
	private EntityType(String message) {
		this.message = message;
	}
}
