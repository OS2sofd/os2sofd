package dk.digitalidentity.sofd.service.model.enums;

public enum UsernameViolation {
	BAD_WORD("html.error.username.badword"),
	EXISTING_USER("html.error.username.existing_user"),
	KNOWN_USERNAME("html.error.username.known_username"),
	RESERVED_BY_OTHER_PERSON("html.error.username.reserved_by_other"),
	PENDING_ORDER_BY_OTHER_PERSON("html.error.username.pending_order_by_other");

	private final String messageKey;

	UsernameViolation(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}
}
