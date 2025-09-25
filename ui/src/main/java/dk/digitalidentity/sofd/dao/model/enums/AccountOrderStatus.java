package dk.digitalidentity.sofd.dao.model.enums;

public enum AccountOrderStatus {
	PENDING("html.enum.accountOrderStatus.pending",false),
	CREATED("html.enum.accountOrderStatus.created",true),
	REACTIVATED("html.enum.accountOrderStatus.reactivated", true),
	DEACTIVATED("html.enum.accountOrderStatus.deactivated", true),
	DELETED("html.enum.accountOrderStatus.deleted", true),
	FAILED("html.enum.accountOrderStatus.failed", false),
	EXPIRED("html.enum.accountOrderStatus.expired", true),
	BLOCKED("html.enum.accountOrderStatus.blocked", false),
	PENDING_APPROVAL("html.enum.accountOrderStatus.pending_approval", false);

	private String messageId;
	private boolean isCompletedStatus;

	private AccountOrderStatus(String messageId, boolean isCompletedStatus) {
		this.messageId = messageId;
		this.isCompletedStatus = isCompletedStatus;
	}

	public String getMessageId() {
		return messageId;
	}

	public boolean isComletedStatus() {
		return isCompletedStatus;
	}
}
