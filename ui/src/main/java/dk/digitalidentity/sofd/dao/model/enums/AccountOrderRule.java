package dk.digitalidentity.sofd.dao.model.enums;

public enum AccountOrderRule {
	UNDECIDED("html.enum.accountorderrole.undecided"),
    DISABLED("html.enum.accountorderrole.disabled"),
    EVERYONE("html.enum.accountorderrole.everyone"),
    EVERYONE_EXCEPT_HOURLY_PAID("html.enum.accountorderrole.everyone_except_hourly_paid"),
    BY_POSITION_NAME("html.enum.accountorderrole.position");

	private String message;
	
	private AccountOrderRule(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
