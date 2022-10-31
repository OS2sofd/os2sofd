package dk.digitalidentity.sofd.dao.model.enums;

public enum AccountOrderStatus {
	PENDING("html.enum.accountOrderStatus.pending"),
	CREATED("html.enum.accountOrderStatus.created"),
	REACTIVATED("html.enum.accountOrderStatus.reactivated"),
	DEACTIVATED("html.enum.accountOrderStatus.deactivated"),
	DELETED("html.enum.accountOrderStatus.deleted"),
	FAILED("html.enum.accountOrderStatus.failed"),
	EXPIRED("html.enum.accountOrderStatus.expired"),
	BLOCKED("html.enum.accountOrderStatus.blocked");

	private String messageId;

	private AccountOrderStatus(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageId() {
		return messageId;
	}
}
