package dk.digitalidentity.sofd.dao.model.enums;

public enum AffiliationDurationRule {
	INHERIT("html.enum.affiliation_duration_rule.inherit"),
	NO_LIMIT("html.enum.affiliation_duration_rule.no_limit"),
	STOP_DATE_REQUIRED("html.enum.affiliation_duration_rule.stop_date_required"),
	MAX_DAYS("html.enum.affiliation_duration_rule.max_days");

	private String message;

	private AffiliationDurationRule(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
