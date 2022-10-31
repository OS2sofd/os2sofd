package dk.digitalidentity.sofd.dao.model.enums;

public enum NotificationType {
	PERSON_WITH_DISALLOWED_ACCOUNT("html.enum.notificationtype.person_with_disallowed_account"),
	PERSON_PERMANENTLY_DELETED("html.enum.notificationtype.person_permanently_deleted", 30),
	ORGUNIT_WITH_MISSING_RULES("html.enum.notificationtype.orgunit_with_missing_rules"),
	ORGUNIT_DELETED("html.enum.notificationtype.orgunit_deleted"),
	ACCOUNT_ORDER_FAILURE("html.enum.notificationtype.account_order_failure", 30),
	EBOKS_REJECTED("html.enum.notificationtype.eboks_rejected", 30),
	NEW_AFFILIATION_LOCATION("html.enum.notificationtype.new_affiliation_location", 90),
	PERSON_WITH_AD_ACCOUNT_WITH_BAD_EMPLOYEEID("html.enum.notificationtype.ad_with_bad_employee_id");
	
	private String message;
	private int expire;

	private NotificationType(String message) {
		this.message = message;
		this.expire = 0;
	}

	private NotificationType(String message, int days) {
		this.message = message;
		this.expire = days;
	}

	public String getMessage() {
		return message;
	}

	public int getExpire() {
		return expire;
	}
}
