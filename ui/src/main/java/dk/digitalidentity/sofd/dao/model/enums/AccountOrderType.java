package dk.digitalidentity.sofd.dao.model.enums;
public enum AccountOrderType {
	CREATE("html.enum.accountOrderStatus.create"),
	DEACTIVATE("html.enum.accountOrderStatus.deactivate"),
	DELETE("html.enum.accountOrderStatus.delete"),
	EXPIRE("html.enum.accountOrderStatus.expire");

	private String messageId;

	private AccountOrderType(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageId() {
		return messageId;
	}
}
