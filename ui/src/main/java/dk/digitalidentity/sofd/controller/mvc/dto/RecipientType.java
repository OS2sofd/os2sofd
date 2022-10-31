package dk.digitalidentity.sofd.controller.mvc.dto;

public enum RecipientType {
	PERSON("html.enum.recipienttype.person"),
	ORGUNIT("html.enum.recipienttype.orgunit");
	
	private String message;
	
	private RecipientType(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
