package dk.digitalidentity.sofd.dao.model.enums;

public enum NotificationType {
	UNMATCHED_WAGES_ORGUNIT("html.enum.notificationtype.unmatched_wages_orgunit", 0, true),
	DELETED_PARENT_ORGUNIT("html.enum.notificationtype.deleted_parent_orgunit", 0, true),
	PERSON_WITH_DISALLOWED_ACCOUNT("html.enum.notificationtype.person_with_disallowed_account"),
	PERSON_PERMANENTLY_DELETED("html.enum.notificationtype.person_permanently_deleted", 30),
	ORGUNIT_WITH_MISSING_RULES("html.enum.notificationtype.orgunit_with_missing_rules"),
	ORGUNIT_WITH_MISSING_RULES_TITLES("html.enum.notificationtype.orgunit_with_missing_rules_titles"),
	ORGUNIT_DELETED("html.enum.notificationtype.orgunit_deleted"),
	ACCOUNT_ORDER_FAILURE("html.enum.notificationtype.account_order_failure", 30),
	EBOKS_REJECTED("html.enum.notificationtype.eboks_rejected", 30),
	NEW_AFFILIATION_LOCATION("html.enum.notificationtype.new_affiliation_location", 90),
	PERSON_WITH_AD_ACCOUNT_WITH_BAD_EMPLOYEEID("html.enum.notificationtype.ad_with_bad_employee_id"),
	PERSON_WITH_FUTURE_AD_ACCOUNT_WITH_BAD_EMPLOYEEID("html.enum.notificationtype.future_ad_with_bad_employee_id"),
	PERSON_DEAD("html.enum.notificationtype.person_dead"),
	PERSON_DEAD_AD_ONLY("html.enum.notificationtype.person_dead_ad_only", false),
	PERSON_DEAD_ACTIVE_AD_ONLY("html.enum.notificationtype.person_dead_active_ad_only", false),
	PERSON_DISENFRANCHISED("html.enum.notificationtype.person_disenfranchised"),
	PERSON_DISENFRANCHISED_AD_ONLY("html.enum.notificationtype.person_disenfranchised_ad_only", false),
	PERSON_DISENFRANCHISED_ACTIVE_AD_ONLY("html.enum.notificationtype.person_disenfranchised_active_ad_only", false),
	NEW_AFFILIATION_SOFD("html.enum.notificationtype.new_affiliation_sofd"),
	NEW_AFFILIATION_IN_EMPTY_ORGUNIT("html.enum.notificationtype.new_affiliation_empty_orgunit", false),
    MANUAL_NOTIFICATION("html.setting.notification.manual", true, 0, false)
	;

	private String message;
	private int expire;
	private boolean visibleInUI;
	private boolean defaultEnabled = true;

	private NotificationType(String message) {
		this.message = message;
		this.expire = 0;
		this.visibleInUI = true;
	}

	private NotificationType(String message, boolean defaultEnabled) {
		this.message = message;
		this.expire = 0;
		this.visibleInUI = true;
		this.defaultEnabled = defaultEnabled;
	}

	private NotificationType(String message, int days) {
		this.message = message;
		this.expire = days;
		this.visibleInUI = true;
	}
	
	private NotificationType(String message, int days, boolean visibleInUI) {
		this.message = message;
		this.expire = days;
		this.visibleInUI = visibleInUI;
	}

    private NotificationType(String message, boolean defaultEnabled, int days, boolean visibleInUI) {
		this.message = message;
        this.defaultEnabled = defaultEnabled;
		this.expire = days;
		this.visibleInUI = visibleInUI;
	}

	public boolean isVisibleInUI() {
		return visibleInUI;
	}
	
	public String getMessage() {
		return message;
	}

	public int getExpire() {
		return expire;
	}

	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}
}
