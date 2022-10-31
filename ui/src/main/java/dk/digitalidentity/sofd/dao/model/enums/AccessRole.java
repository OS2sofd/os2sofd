package dk.digitalidentity.sofd.dao.model.enums;

public enum AccessRole {
	WRITE_ACCESS("html.enum.accessrole.writeAccess"),
	READ_ACCESS("html.enum.accessrole.readAccess"),
	LIMITED_READ_ACCESS("html.enum.accessrole.limitedReadAccess");

	private String messageId;
	
	private AccessRole(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageId() {
		return messageId;
	}
}
